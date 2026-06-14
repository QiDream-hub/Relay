package qdream.relay.core;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import qdream.relay.blocks.entity.custom.ShellBlockEntity;

/**
 * 核心组
 * 处理相邻核心的合并逻辑
 * 物理相邻的核心自动合并：总核心数累加，interval = max(所有核心)
 */
public class CoreGroup {
    private final Set<ShellBlockEntity> shells;
    private int totalCoreCount;
    private int maxInterval;

    public CoreGroup() {
        this.shells = new HashSet<>();
        this.totalCoreCount = 0;
        this.maxInterval = 1;
    }

    /**
     * 从世界中检测并合并相邻的核心
     */
    public static CoreGroup fromWorld(Level level, BlockPos startPos) {
        CoreGroup group = new CoreGroup();
        Set<BlockPos> visited = new HashSet<>();
        discoverCores(level, startPos, group, visited);
        return group;
    }

    /**
     * 递归发现相邻的核心
     */
    private static void discoverCores(Level level, BlockPos pos, CoreGroup group, Set<BlockPos> visited) {
        if (visited.contains(pos)) {
            return;
        }
        visited.add(pos);

        BlockState state = level.getBlockState(pos);
        // TODO: 检查是否为外壳方块
        // if (!(state.getBlock() instanceof ShellBlock)) {
        //     return;
        // }

        if (level.getBlockEntity(pos) instanceof ShellBlockEntity shell) {
            group.addShell(shell);
            
            // 检查六个相邻方向
            for (var direction : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                discoverCores(level, neighbor, group, visited);
            }
        }
    }

    /**
     * 添加外壳到组中
     */
    public void addShell(ShellBlockEntity shell) {
        if (shells.contains(shell)) {
            return;
        }
        shells.add(shell);
        
        // 累加核心数
        totalCoreCount += shell.getCoreCount();
        
        // 取最大 interval
        maxInterval = Math.max(maxInterval, shell.getInterval());
    }

    /**
     * 移除外壳
     */
    public void removeShell(ShellBlockEntity shell) {
        if (shells.remove(shell)) {
            recalculate();
        }
    }

    /**
     * 重新计算核心组的属性
     */
    public void recalculate() {
        totalCoreCount = 0;
        maxInterval = 1;
        
        for (ShellBlockEntity shell : shells) {
            totalCoreCount += shell.getCoreCount();
            maxInterval = Math.max(maxInterval, shell.getInterval());
        }
    }

    /**
     * 获取总核心数
     */
    public int getTotalCoreCount() {
        return totalCoreCount;
    }

    /**
     * 获取合并后的 interval
     */
    public int getMergedInterval() {
        return maxInterval;
    }

    /**
     * 获取所有外壳
     */
    public Set<ShellBlockEntity> getShells() {
        return Set.copyOf(shells);
    }

    /**
     * 获取组中外壳数量
     */
    public int size() {
        return shells.size();
    }

    /**
     * 是否为空组
     */
    public boolean isEmpty() {
        return shells.isEmpty();
    }

    /**
     * 清除组
     */
    public void clear() {
        shells.clear();
        totalCoreCount = 0;
        maxInterval = 1;
    }

    @Override
    public String toString() {
        return "CoreGroup[shells=" + shells.size() + ", cores=" + totalCoreCount + ", interval=" + maxInterval + "]";
    }
}
