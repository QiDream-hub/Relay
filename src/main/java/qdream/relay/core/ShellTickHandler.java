package qdream.relay.core;

import net.minecraft.nbt.CompoundTag;
import qdream.relay.Relay;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Warning;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Instruction;

/**
 * 外壳 Tick 处理器
 * 处理三种外壳（方块/实体/工具）的通用 tick 逻辑
 *
 * <p>
 * 所有状态访问都通过 {@link ShellContainer} 接口进行，不直接访问物品或内部字段。
 * </p>
 */
public class ShellTickHandler {

    /**
     * 调试回调接口 - 用于监视 StateMachine 的执行
     */
    public interface DebugCallback {
        /**
         * 在执行操作前调用
         *
         * @param stateMachine 被监视的状态机
         * @param executable   当前执行的可执行单元
         */
        default void beforeStep(StateMachine stateMachine, Executable executable) {
        }

        /**
         * 在操作成功执行后调用
         *
         * @param stateMachine 被监视的状态机
         * @param executable   当前执行的可执行单元
         */
        default void afterStep(StateMachine stateMachine, Executable executable) {
        }

        /**
         * 在操作触发事故时调用
         *
         * @param stateMachine 被监视的状态机
         * @param executable   当前执行的可执行单元
         * @param warning      警告对象
         */
        default void onMishap(StateMachine stateMachine, Executable executable, Warning warning) {
        }
    }

    private int tickCounter;
    private boolean initialized;
    private DebugCallback debugCallback;

    public ShellTickHandler() {
        this.tickCounter = 0;
        this.initialized = false;
        this.debugCallback = null;
    }

    /**
     * 执行 tick
     *
     * @param container 外壳容器
     */
    public void tick(ShellContainer container) {
        if (container.isClientSide()) {
            return;
        }

        // 检查是否可以执行（已启用 + 已初始化 + 正在运行）
        if (!container.canExecute()) {
            return;
        }

        container.getStateMachine().setContext("shellContainer", container);

        // 执行状态机
        tickCounter++;
        int interval = container.getInterval();
        int coreCost = container.getCoreCost();

        if (tickCounter >= interval && coreCost > 0) {
            tickCounter = 0;

            // 执行 tick - mc 层负责控制执行节奏和能量扣除
            runTick(container, coreCost);

            // 统计：tick 次数
            ExecutionStats stats = container.getExecutionStats();
            stats.incrementRunCount();
        }
    }

    /**
     * 累计的 coreCost，用于支持高 cost 操作
     * 当单个操作需要的 cost 超过单 tick 的 coreCost 时，会累积多 tick 再执行
     */
    private int accumulatedCost = 0;

    /**
     * 获取累计的 coreCost
     *
     * @return 累计的 coreCost 值
     */
    public int getAccumulatedCost() {
        return accumulatedCost;
    }

    /**
     * 设置累计的 coreCost
     *
     * @param accumulatedCost 累计的 coreCost 值
     */
    public void setAccumulatedCost(int accumulatedCost) {
        this.accumulatedCost = accumulatedCost;
    }

    /**
     * 执行一个 tick 的逻辑
     * mc 层负责：控制每 tick 执行的操作数、扣除能量
     *
     * @param container 外壳容器
     * @param coreCost  核心数量（每 tick 最大可执行操作数）
     */
    private void runTick(ShellContainer container, int coreCost) {
        var stateMachine = container.getStateMachine();
        double energyCostPerTick = container.getEnergyCostPerTick();
        ExecutionStats stats = container.getExecutionStats();

        int executedOps = 0;

        // 检查能量（核心基础消耗）- 每 tick 只检查一次
        if (!container.hasEnoughEnergy(energyCostPerTick)) {
            stateMachine.triggerMishap(new qdream.relay.mc.errors.EnergyException(
                    stateMachine,
                    net.minecraft.network.chat.Component.translatable("error.energy_insufficient", energyCostPerTick,
                            container.getEnergy())));
            accumulatedCost = 0;
            return;
        }

        // 累加本 tick 的 coreCost
        accumulatedCost += coreCost;

        while (stateMachine.isRunning()) {
            // 预检查栈顶操作的 cost
            Executable top = stateMachine.peekProgram();
            if (top == null) {
                break;
            }

            int cost = 1; // 默认 cost
            if (top instanceof Operation op) {
                cost = op.getCost();
            }

            if (cost > accumulatedCost) {
                // cost 不足，保留剩余的 accumulatedCost 到下一 tick
                break;
            }

            // 检查操作能量（在消耗 accumulatedCost 之前检查）
            if (top instanceof Instruction spell) {
                double opEnergy = spell.getEnergy();
                if (!container.hasEnoughEnergy(opEnergy)) {
                    stateMachine.triggerMishap(new qdream.relay.mc.errors.EnergyException(
                            stateMachine,
                            net.minecraft.network.chat.Component.translatable("error.energy_insufficient", opEnergy,
                                    container.getEnergy())));
                    accumulatedCost = 0;
                    return;
                }
            }

            // 执行单个操作
            Executable currentOp = top;

            // 调试回调 - 执行前
            if (debugCallback != null) {
                debugCallback.beforeStep(stateMachine, currentOp);
            }

            try {
                if (!stateMachine.step()) {
                    break;
                }
            } catch (Exception e) {
                Relay.LOGGER.error(e.getMessage(), e);
                break;
            }

            // 调试回调 - 执行后
            if (debugCallback != null) {
                debugCallback.afterStep(stateMachine, currentOp);
            }

            // 扣除操作能量并统计
            if (top instanceof Instruction spell) {
                double opEnergy = spell.getEnergy();
                container.consumeEnergy(opEnergy);
                stats.addOperationEnergy(opEnergy);
            }

            executedOps++;
            accumulatedCost -= cost;
        }

        // 统计：核心基础能量消耗 = 本 tick 实际消耗的 accumulatedCost
        if (executedOps > 0) {
            container.consumeEnergy(energyCostPerTick);
            stats.addCoreEnergy(energyCostPerTick);
        }

        // 统计：操作执行次数
        stats.incrementOperations(executedOps);

        // 程序执行完毕后，重置初始化状态和累计 cost
        if (!stateMachine.isRunning()) {
            accumulatedCost = 0;
        }

        container.setChanged();
    }

    // ========== Getter/Setter ==========

    public int getTickCounter() {
        return tickCounter;
    }

    public void setTickCounter(int tickCounter) {
        this.tickCounter = tickCounter;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * 设置调试回调
     *
     * @param callback 调试回调，为 null 时禁用调试
     */
    public void setDebugCallback(DebugCallback callback) {
        this.debugCallback = callback;
    }

    /**
     * 获取调试回调
     *
     * @return 当前的调试回调
     */
    public DebugCallback getDebugCallback() {
        return debugCallback;
    }

    // ========== NBT 序列化 ==========

    /**
     * 将 TickHandler 状态序列化为 NBT
     *
     * @return 包含状态数据的 CompoundTag
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("tickCounter", tickCounter);
        tag.putBoolean("initialized", initialized);
        tag.putInt("accumulatedCost", accumulatedCost);
        return tag;
    }

    /**
     * 从 NBT 加载 TickHandler 状态
     *
     * @param tag 包含状态数据的 CompoundTag
     */
    public void fromNbt(CompoundTag tag) {
        this.tickCounter = tag.getInt("tickCounter").orElse(0);
        this.initialized = tag.getBoolean("initialized").orElse(false);
        this.accumulatedCost = tag.getInt("accumulatedCost").orElse(0);
    }
}
