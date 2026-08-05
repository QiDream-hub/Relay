package qdream.relay.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.blocks.entity.custom.BlockShellEntity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * 外壳核心组管理器
 *
 * <h3>职责</h3>
 * <ul>
 * <li>提供 {@link ShellCoreGroupSavedData} 的访问入口</li>
 * <li>在 BlockShell 放置时检测相邻方块并加入/合并组</li>
 * <li>在 BlockShell 破坏时从组中移除，处理组拆分</li>
 * <li>为 {@link BlockShellEntity} 提供组查询接口（用于计算 cost/interval）</li>
 * </ul>
 *
 * <h3>设计原则</h3>
 * <p>BlockShellEntity 的 coreGroupId 是权威标识 - 每个方块知道自己属于哪个组。</p>
 * <p>SavedData 只存储组 UUID -> 成员坐标列表的映射，不维护反向索引。</p>
 *
 * <h3>使用示例</h3>
 *
 * <pre>
 * // 放置时加入组
 * ShellCoreGroupManager.onBlockPlaced(level, pos, shell);
 *
 * // 破坏时移除
 * ShellCoreGroupManager.onBlockRemoved(level, pos, shell);
 *
 * // 获取组成员（用于计算 cost/interval）
 * List&lt;BlockPos&gt; members = ShellCoreGroupManager.getGroupMembers(level, groupId);
 * </pre>
 */
@NullMarked
public class ShellCoreGroupManager {

    /**
     * 获取或创建 SavedData
     * 每个维度有自己独立的 SavedData 实例
     */
    public static @Nullable ShellCoreGroupSavedData getOrCreate(Level level) {
        if (level.isClientSide()) {
            return null;
        }

        return ((ServerLevel) level).getDataStorage().computeIfAbsent(ShellCoreGroupSavedData.TYPE);
    }

    /**
     * 当方块被放置时调用，检测相邻方块并加入/合并组
     * 
     * <p>此方法只更新 SavedData，不反向更新方块的 coreGroupId 字段。</p>
     * <p>方块在读取时通过 {@link #getGroupIdForPosition} 查询自己的组 ID。</p>
     */
    public static void onBlockPlaced(Level level, BlockPos pos, BlockShellEntity shell) {
        if (level.isClientSide()) {
            return;
        }

        ShellCoreGroupSavedData data = getOrCreate(level);
        if (data == null) {
            return;
        }

        // 收集所有相邻方块的组 ID（6 向检查）
        Set<UUID> adjacentGroupIds = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof BlockShellEntity neighborShell) {
                // 直接从 SavedData 查询邻居的组 ID，保证数据一致性
                UUID neighborGroupId = getGroupIdForPosition(level, neighborPos);
                if (neighborGroupId != null) {
                    adjacentGroupIds.add(neighborGroupId);
                }
            }
        }

        if (adjacentGroupIds.isEmpty()) {
            // 没有相邻组，创建新组
            UUID newGroupId = UUID.randomUUID();
            data.createGroup(newGroupId, pos);
            shell.setCoreGroupId(newGroupId); // 仅设置当前方块
        } else {
            // 有相邻组，加入第一个组
            Iterator<UUID> iterator = adjacentGroupIds.iterator();
            UUID targetGroupId = iterator.next();
            data.addToGroup(targetGroupId, pos);
            shell.setCoreGroupId(targetGroupId); // 仅设置当前方块

            // 如果检测到多个不同的组，合并它们
            while (iterator.hasNext()) {
                UUID otherGroupId = iterator.next();
                data.mergeGroups(targetGroupId, otherGroupId);
            }
            // 不再反向更新所有成员的 coreGroupId - 方块在读取时动态查询
        }
    }

    /**
     * 当方块被破坏时调用，从组中移除并处理组拆分
     * 
     * <p>此方法只更新 SavedData，不反向更新方块的 coreGroupId 字段。</p>
     * <p>方块在读取时通过 {@link #getGroupIdForPosition} 查询自己的组 ID。</p>
     */
    public static void onBlockRemoved(Level level, BlockPos pos, BlockShellEntity shell) {
        if (level.isClientSide()) {
            return;
        }

        ShellCoreGroupSavedData data = getOrCreate(level);
        if (data == null) {
            return;
        }

        // 从 SavedData 获取组 ID（权威来源），而不是从 shell 获取
        UUID oldGroupId = getGroupIdForPosition(level, pos);
        if (oldGroupId == null) {
            return;
        }

        // 从组中移除（可能导致拆分）
        data.removeFromGroup(pos);
        // 不再反向更新所有受影响方块的 coreGroupId - 方块在读取时动态查询
    }

    /**
     * 获取指定坐标所属的组 ID
     * 委托给 SavedData 的公共方法
     * 
     * @param level 世界
     * @param pos   坐标
     * @return 组 ID，如果不属于任何组则返回 null
     */
    public static @Nullable UUID getGroupIdForPosition(Level level, BlockPos pos) {
        if (level.isClientSide() || pos == null) {
            return null;
        }

        ShellCoreGroupSavedData data = getOrCreate(level);
        if (data == null) {
            return null;
        }

        // 使用 SavedData 的公共方法查询
        return data.getGroupIdForPosition(pos);
    }

    /**
     * 获取组的所有成员坐标
     *
     * @param level   世界
     * @param groupId 组 ID
     * @return 成员坐标列表
     */
    public static List<BlockPos> getGroupMembers(Level level, UUID groupId) {
        if (level.isClientSide() || groupId == null) {
            return Collections.emptyList();
        }

        ShellCoreGroupSavedData data = getOrCreate(level);
        if (data == null) {
            return Collections.emptyList();
        }

        return data.getGroupMembers(groupId);
    }

    /**
     * 获取组的大小
     */
    public static int getGroupSize(Level level, UUID groupId) {
        if (level.isClientSide() || groupId == null) {
            return 0;
        }

        ShellCoreGroupSavedData data = getOrCreate(level);
        if (data == null) {
            return 0;
        }

        return data.getGroupSize(groupId);
    }

    /**
     * 检查坐标是否属于某个组
     */
    public static boolean isInGroup(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return false;
        }

        ShellCoreGroupSavedData data = getOrCreate(level);
        if (data == null) {
            return false;
        }

        return data.isInGroup(pos);
    }
}
