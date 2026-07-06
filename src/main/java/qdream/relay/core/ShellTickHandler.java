package qdream.relay.core;

import net.minecraft.world.item.ItemStack;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.container.ToolShellContainer;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.component.EnergyModuleComponent;

/**
 * 外壳 Tick 处理器
 * 处理三种外壳（方块/实体/工具）的通用 tick 逻辑
 * 
 * <p>所有状态访问都通过 {@link ShellContainer} 接口进行，不直接访问物品或内部字段。</p>
 */
public class ShellTickHandler {

    /**
     * 调试回调接口 - 用于监视 StateMachine 的栈变化
     */
    @FunctionalInterface
    public interface DebugCallback {
        /**
         * 当 StateMachine 发生栈修改时调用
         *
         * @param stateMachine 被监视的状态机
         * @param phase        执行阶段 ("beforeStep" / "afterStep" / "mishap")
         * @param executable   当前执行的可执行单元
         */
        void onStackChange(StateMachine stateMachine, String phase, Executable executable);
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

        // 未启用时不执行
        if (!container.isEnabled()) {
            return;
        }

        // 从 container 同步 initialized 状态（权威来源）
        this.initialized = container.isInitialized();

        // 执行状态机
        if (initialized && container.isRunning()) {
            tickCounter++;
            int interval = container.getInterval();
            int coreCount = container.getCoreCost();
            
            if (tickCounter >= interval && coreCount > 0) {
                tickCounter = 0;

                // 设置上下文 - 传递世界交互器等信息给操作
                var stateMachine = container.getStateMachine();
                stateMachine.setContext("shellContainer", container);

                // 执行 tick - mc 层负责控制执行节奏和能量扣除
                runTick(container, coreCount);
            }
        }
    }

    /**
     * 执行一个 tick 的逻辑
     * mc 层负责：控制每 tick 执行的操作数、扣除能量
     *
     * @param container 外壳容器
     * @param coreCount 核心数量（每 tick 最大可执行操作数）
     */
    private void runTick(ShellContainer container, int coreCount) {
        var stateMachine = container.getStateMachine();
        double energyCostPerTick = container.getEnergyCostPerTick();

        int usedCost = 0;

        while (usedCost < coreCount && stateMachine.isRunning()) {
            // 预检查栈顶操作的 cost
            Executable top = stateMachine.peekProgram();
            if (top == null) {
                break;
            }

            int cost = 1; // 默认 cost
            if (top instanceof Operation op) {
                cost = op.getCost();
            }

            if (usedCost + cost > coreCount) {
                break; // cost 不足，等待下 tick
            }

            // 检查能量（核心基础消耗）
            if (!container.hasEnoughEnergy(energyCostPerTick)) {
                stateMachine.triggerMishap("能量不足：需要 " + energyCostPerTick + "，当前只有 " + container.getEnergy());
                return;
            }

            // 执行单个操作
            Executable currentOp = top;

            // 调试回调 - 执行前
            if (debugCallback != null) {
                debugCallback.onStackChange(stateMachine, "beforeStep", currentOp);
            }

            if (!stateMachine.step()) {
                // 调试回调 - 执行失败
                if (debugCallback != null) {
                    debugCallback.onStackChange(stateMachine, "mishap", currentOp);
                }
                break; // 执行失败
            }

            // 调试回调 - 执行后
            if (debugCallback != null) {
                debugCallback.onStackChange(stateMachine, "afterStep", currentOp);
            }

            // 扣除能量 - 通过 container 接口，支持背包能量模块
            container.consumeEnergy(energyCostPerTick);
            usedCost += cost;
        }

        // 程序执行完毕后，清空数据栈（避免下次运行时使用遗留数据）
        if (!stateMachine.isRunning()) {
            stateMachine.clear();
            container.setInitialized(false);
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
}
