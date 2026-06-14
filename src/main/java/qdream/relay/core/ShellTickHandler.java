package qdream.relay.core;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import qdream.relay.engine.Executable;
import qdream.relay.items.RelayDataComponents;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;

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

        // 检查磁盘是否变更，如果变更则重置初始化状态
        if (initialized && !container.getDiskStack().equals(lastDisk)) {
            initialized = false;
            container.setInitialized(false);
            container.getStateMachine().clear();
        }
        lastDisk = container.getDiskStack().copy();

        // 尝试初始化
        if (!initialized) {
            tryInitialize(container);
        }

        // 执行状态机
        if (initialized && coreCount > 0 && container.getStateMachine().isRunning()) {
            tickCounter++;
            if (tickCounter >= interval) {
                tickCounter = 0;
                container.getStateMachine().run(coreCount);
                container.setChanged();
            }
        }
    }

    // 记录上一个磁盘，用于检测变更
    private net.minecraft.world.item.ItemStack lastDisk = net.minecraft.world.item.ItemStack.EMPTY;

    /**
     * 更新核心状态
     */
    private void updateCoreState(ShellContainer container) {
        ItemStack coreStack = container.getCoreStack();
        if (!coreStack.isEmpty()) {
            // 简单实现：单个核心，interval=1
            // 后续由 CoreGroup 处理合并逻辑
            coreCount = 1;
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

    /**
     * 尝试初始化 - 从法术磁盘加载程序
     */
    private void tryInitialize(ShellContainer container) {
        ItemStack diskStack = container.getDiskStack();
        if (!diskStack.isEmpty()) {
            CompoundTag compoundTag = diskStack.get(qdream.relay.items.RelayDataComponents.SPELL_PROGRAM);
            if (compoundTag != null) {
                ListTag programTag = compoundTag.getList("program").orElse(null);
                if (programTag != null && !programTag.isEmpty()) {
                    List<Executable> fromNbt;
                    try {
                        fromNbt = qdream.relay.mc.ProgramCompiler.fromNbt(programTag);
                    } catch (qdream.relay.mc.ProgramCompiler.CompilationException e) {
                        fromNbt = List.of();
                        e.printStackTrace();
                    }
                    container.getStateMachine().loadProgram(fromNbt);
                    // 只有成功加载程序后才设置 initialized
                    container.setInitialized(true);
                    initialized = true;
                }
            }
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
