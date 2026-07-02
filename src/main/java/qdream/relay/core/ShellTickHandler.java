package qdream.relay.core;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.engine.Executable;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.items.ToolShellContainer;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;

/**
 * 外壳 Tick 处理器
 * 处理三种外壳（方块/实体/工具）的通用 tick 逻辑
 */
public class ShellTickHandler {

    private int tickCounter;
    private int interval;
    private int coreCount;
    private double energyCost; // 核心能量消耗
    private boolean initialized;

    public ShellTickHandler() {
        this.tickCounter = 0;
        this.interval = 1;
        this.coreCount = 0;
        this.energyCost = 0;
        this.initialized = false;
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

        // 更新能量状态
        updateEnergy(container);

        // 更新核心状态
        updateCoreState(container);

        // 从 container 同步 initialized 状态（关键修复：每次 tick 使用 container 的状态）
        this.initialized = container.isInitialized();

        // 执行状态机
        if (initialized && coreCount > 0 && interval > 0 && container.getStateMachine().isRunning()) {
            tickCounter++;
            if (tickCounter >= interval) {
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
     * @param maxOps    本 tick 最大可执行操作数（由核心数量决定）
     */
    private void runTick(ShellContainer container, int maxOps) {
        var stateMachine = container.getStateMachine();
        double currentEnergy = container.getEnergy();

        int usedCost = 0;

        while (usedCost < maxOps && stateMachine.isRunning()) {
            // 预检查栈顶操作的 cost
            Executable top = stateMachine.peekProgram();
            if (top == null) {
                break;
            }

            int cost = 1; // 默认 cost
            if (top instanceof Operation op) {
                cost = op.getCost();
            }

            if (usedCost + cost > maxOps) {
                break; // cost 不足，等待下 tick
            }

            // 检查并扣除能量（核心基础消耗 + 操作消耗）
            double required = energyCost; // 核心基础消耗
            if (top instanceof Spell spell) {
                required += spell.getEnergy(); // 加上操作消耗
            } else {
                required += cost; // 非 Spell 操作按 cost 扣除
            }

            if (currentEnergy < required) {
                stateMachine.triggerMishap("能量不足：需要 " + required + "，当前只有 " + currentEnergy);
                return;
            }

            // 执行单个操作
            if (!stateMachine.step()) {
                break; // 执行失败
            }

            // 扣除能量 - 使用 container 的方法，支持背包能量模块
            double consumed = 0;
            if (container instanceof ToolShellContainer toolShell) {
                consumed = toolShell.consumeEnergy(required);
            } else {
                ItemStack energyStack = container.getEnergyStack();
                if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
                    consumed = emi.consumeEnergy(energyStack, required);
                }
            }
            currentEnergy = container.getEnergy();
            usedCost += cost;
        }

        // 程序执行完毕后，清空数据栈（避免下次运行时使用遗留数据）
        if (!stateMachine.isRunning()) {
            stateMachine.clear();
            container.setInitialized(false);
        }

        container.setChanged();
    }

    /**
     * 更新核心状态
     */
    public void updateCoreState(ShellContainer container) {
        ItemStack coreStack = container.getCoreStack();
        if (!coreStack.isEmpty()) {
            coreCount = coreStack.count();
            // 从核心物品中读取 interval 和 energyCost 属性
            if (coreStack.getItem() instanceof ComputingCoreComponent core) {
                interval = core.getInterval(coreStack);
                energyCost = core.getEnergyCost(coreStack);
            } else {
                interval = 0;
                energyCost = 0;
            }
        } else {
            coreCount = 0;
            interval = 0;
            energyCost = 0;
        }
    }

    /**
     * 更新能量状态
     */
    public void updateEnergy(ShellContainer container) {
        ItemStack energyStack = container.getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            double storedEnergy = emi.getStoredEnergy(energyStack);
            container.setEnergy(storedEnergy);
        } else {
            container.setEnergy(0);
        }
    }

    // ========== Getter/Setter ==========

    public int getTickCounter() {
        return tickCounter;
    }

    public void setTickCounter(int tickCounter) {
        this.tickCounter = tickCounter;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getCoreCount() {
        return coreCount;
    }

    public void setCoreCount(int coreCount) {
        this.coreCount = coreCount;
    }

    public double getEnergyCost() {
        return energyCost;
    }

    public void setEnergyCost(double energyCost) {
        this.energyCost = energyCost;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
