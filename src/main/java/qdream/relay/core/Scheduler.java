package qdream.relay.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.Level;
import qdream.relay.blocks.entity.custom.ShellBlockEntity;
import net.minecraft.core.BlockPos;

/**
 * 调度器
 * 按 interval 调度状态机执行
 */
public class Scheduler {
    
    private final Level level;
    private final List<ScheduledShell> scheduledShells;
    
    public Scheduler(Level level) {
        this.level = level;
        this.scheduledShells = new ArrayList<>();
    }

    /**
     * 注册外壳到调度器
     */
    public void schedule(ShellBlockEntity shell, BlockPos pos) {
        scheduledShells.add(new ScheduledShell(shell, pos));
    }

    /**
     * 从调度器移除外壳
     */
    public void unschedule(ShellBlockEntity shell) {
        scheduledShells.removeIf(s -> s.shell == shell);
    }

    /**
     * 执行 tick
     * 遍历所有外壳，检查 interval 并执行状态机
     */
    public void tick() {
        if (level.isClientSide()) {
            return;
        }
        
        List<ScheduledShell> toRemove = new ArrayList<>();
        
        for (ScheduledShell scheduled : scheduledShells) {
            if (scheduled.shell.isRemoved()) {
                toRemove.add(scheduled);
                continue;
            }
            
            // 检查外壳是否仍然有效
            if (!scheduled.shell.hasLevel() || scheduled.shell.getLevel() != level) {
                toRemove.add(scheduled);
                continue;
            }
            
            BlockPos currentPos = scheduled.shell.getBlockPos();
            if (!currentPos.equals(scheduled.pos)) {
                // 外壳已移动，更新位置
                scheduled.pos = currentPos;
            }
            
            // 检查是否需要执行
            if (scheduled.shell.isInitialized() && scheduled.shell.getCoreCount() > 0) {
                // 更新 tick 计数器
                scheduled.tickCounter++;
                if (scheduled.tickCounter >= scheduled.shell.getInterval()) {
                    scheduled.tickCounter = 0;
                    
                    // 执行状态机 tick
                    scheduled.shell.getStateMachine().run(scheduled.shell.getCoreCount());
                    scheduled.shell.setChanged();
                }
            }
        }
        
        // 清理无效的外壳
        scheduledShells.removeAll(toRemove);
    }

    /**
     * 获取调度的外壳数量
     */
    public int size() {
        return scheduledShells.size();
    }

    /**
     * 清除所有调度
     */
    public void clear() {
        scheduledShells.clear();
    }

    /**
     * 内部类：调度的外壳
     */
    private static class ScheduledShell {
        final ShellBlockEntity shell;
        BlockPos pos;
        int tickCounter;
        
        ScheduledShell(ShellBlockEntity shell, BlockPos pos) {
            this.shell = shell;
            this.pos = pos;
            this.tickCounter = 0;
        }
    }
}
