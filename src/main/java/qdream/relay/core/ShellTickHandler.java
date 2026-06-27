package qdream.relay.core;

import net.minecraft.world.item.ItemStack;

/**
 * 外壳 Tick 处理器
 * 处理三种外壳（方块/实体/工具）的通用 tick 逻辑
 */
public class ShellTickHandler {

    private int tickCounter;
    private int interval;
    private int coreCount;
    private boolean initialized;

    public ShellTickHandler() {
        this.tickCounter = 0;
        this.interval = 1;
        this.coreCount = 0;
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

        // 更新核心状态
        updateCoreState(container);

        // 更新能量状态
        updateEnergy(container);

        // 执行状态机
        if (initialized && coreCount > 0 && container.getStateMachine().isRunning()) {
            tickCounter++;
            if (tickCounter >= interval) {
                tickCounter = 0;

                // 设置上下文 - 传递世界交互器等信息给操作
                var stateMachine = container.getStateMachine();
                stateMachine.setContext("worldInteractor", container.getInteractorStack());
                stateMachine.setContext("shellContainer", container);

                // 获取世界引用并设置到上下文
                if (container instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
                    stateMachine.setContext("world", blockEntity.getLevel());
                } else if (container instanceof net.minecraft.world.entity.Entity entity) {
                    stateMachine.setContext("world", entity.level());
                }

                // 执行
                stateMachine.run(coreCount);

                // 可选：清空上下文（如果不需要持久化）
                stateMachine.clearContext();

                container.setChanged();
            }
        }
    }

    /**
     * 更新核心状态
     */
    private void updateCoreState(ShellContainer container) {
        ItemStack coreStack = container.getCoreStack();
        if (!coreStack.isEmpty()) {
            // 简单实现：单个核心，interval=1
            // 后续由 CoreGroup 处理合并逻辑
            coreCount = 10;
            interval = 1;
        } else {
            coreCount = 0;
            interval = 1;
        }
    }

    /**
     * 更新能量状态
     */
    private void updateEnergy(ShellContainer container) {
        ItemStack energyStack = container.getEnergyStack();
        if (!energyStack.isEmpty()) {
            // TODO: 从能量模块读取能量
            container.setEnergy(1000);
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

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
