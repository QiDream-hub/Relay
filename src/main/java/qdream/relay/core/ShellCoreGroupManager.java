package qdream.relay.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.blocks.entity.custom.BlockShellEntity;

import java.util.*;

/**
 * 外壳核心组管理器
 * 
 * <h3>职责</h3>
 * <ul>
 * <li>提供 {@link ShellCoreGroupSavedData} 的访问入口</li>
 * <li>在 BlockShell 放置时检测相邻方块并加入/合并组</li>
 * <li>在 BlockShell 破坏时从组中移除</li>
 * <li>为 {@link BlockShellEntity} 提供组查询接口（用于计算 cost/interval）</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
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
public class ShellCoreGroupManager {

    /**
     * 获取或创建 SavedData
     * 每个维度有自己独立的 SavedData 实例
     */
    public static ShellCoreGroupSavedData getOrCreate(Level level) {
        if (level.isClientSide()) {
            return null;
        }

        // 使用当前维度的数据存储，而不是主世界
        return ((ServerLevel) level).getDataStorage().computeIfAbsent(ShellCoreGroupSavedData.TYPE);
    }

    /**
     * 当方块被放置时调用，检测相邻方块并加入/合并组
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
            UUID neighborGroupId = data.getGroupIdAt(neighborPos);
            if (neighborGroupId != null) {
                adjacentGroupIds.add(neighborGroupId);
            }
        }
        
        if (adjacentGroupIds.isEmpty()) {
            // 没有相邻组，创建新组
            UUID newGroupId = UUID.randomUUID();
            data.createGroup(newGroupId, pos);
            shell.setCoreGroupId(newGroupId);
        } else {
            // 有相邻组，加入第一个组
            Iterator<UUID> iterator = adjacentGroupIds.iterator();
            UUID targetGroupId = iterator.next();
            data.addToGroup(targetGroupId, pos);
            shell.setCoreGroupId(targetGroupId);
            
            // 如果检测到多个不同的组，合并它们
            while (iterator.hasNext()) {
                UUID otherGroupId = iterator.next();
                data.mergeGroups(targetGroupId, otherGroupId);
            }
        }
    }

    /**
     * 当方块被破坏时调用，从组中移除
     */
    public static void onBlockRemoved(Level level, BlockPos pos, BlockShellEntity shell) {
        if (level.isClientSide()) {
            return;
        }

        ShellCoreGroupSavedData data = getOrCreate(level);
        if (data == null) {
            return;
        }

        UUID oldGroupId = shell.getCoreGroupId();
        if (oldGroupId != null) {
            // 获取移除前的组成员
            List<BlockPos> oldMembers = new ArrayList<>(data.getGroupMembers(oldGroupId));
            
            // 从组中移除（可能导致拆分）
            data.removeFromGroup(pos);
            
            // 检查是否发生了拆分：如果移除后还有成员，且它们不再属于旧组，说明发生了拆分
            if (!oldMembers.isEmpty()) {
                // 收集所有需要更新的 BlockShell
                Set<BlockPos> toUpdate = new HashSet<>();
                for (BlockPos memberPos : oldMembers) {
                    if (!memberPos.equals(pos)) {
                        toUpdate.add(memberPos);
                    }
                }
                
                // 更新所有剩余成员的 coreGroupId
                for (BlockPos memberPos : toUpdate) {
                    BlockEntity be = level.getBlockEntity(memberPos);
                    if (be instanceof BlockShellEntity memberShell) {
                        UUID newGroupId = data.getGroupIdAt(memberPos);
                        if (newGroupId != null && !newGroupId.equals(oldGroupId)) {
                            memberShell.setCoreGroupId(newGroupId);
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取组的所有成员坐标
     * @param level 世界
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
     * 获取坐标所属的组 ID
     */
    public static UUID getGroupIdAt(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return null;
        }
        
        ShellCoreGroupSavedData data = getOrCreate(level);
        if (data == null) {
            return null;
        }
        
        return data.getGroupIdAt(pos);
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
