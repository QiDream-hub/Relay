package qdream.relay.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * 外壳核心组保存数据
 *
 * <h3>设计</h3>
 * <p>简化设计：只保留组 UUID -> 成员坐标列表的映射，不维护反向索引。</p>
 * <p>BlockShellEntity 的 coreGroupId 字段是权威标识 - 每个方块知道自己属于哪个组。</p>
 *
 * <h3>数据结构</h3>
 * <pre>
 * {
 *   "groups": [
 *     {
 *       "id": "uuid-1",
 *       "positions": [
 *         { "x": 100, "y": 64, "z": 200 },
 *         { "x": 101, "y": 64, "z": 200 }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * <h3>使用方式</h3>
 * <ul>
 * <li>放置 BlockShell 时：检查 6 向相邻方块，加入第一个扫描到的组，如果还有其他组则合并</li>
 * <li>获取 cost/interval 时：通过 coreGroupId 直接获取整个组的所有成员坐标，然后遍历计算</li>
 * <li>移除时：如果导致组拆分，为每个新连通分量创建新组，并更新对应方块的 coreGroupId</li>
 * </ul>
 */
@NullMarked
public class ShellCoreGroupSavedData extends SavedData {

    public static final SavedDataType<ShellCoreGroupSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("relay", "shell_core_groups"),
        ShellCoreGroupSavedData::new,
        CompoundTag.CODEC.xmap(ShellCoreGroupSavedData::load, ShellCoreGroupSavedData::save),
        null
    );

    /**
     * 组 UUID -> 成员坐标列表
     */
    private final Map<UUID, List<BlockPos>> groups;

    public ShellCoreGroupSavedData() {
        this.groups = new HashMap<>();
    }

    /**
     * 从 NBT 加载
     */
    public static ShellCoreGroupSavedData load(CompoundTag tag) {
        ShellCoreGroupSavedData data = new ShellCoreGroupSavedData();

        tag.getList("groups").ifPresent(groupsListTag -> {
            for (int i = 0; i < groupsListTag.size(); i++) {
                CompoundTag groupTag = groupsListTag.getCompound(i).orElse(null);
                if (groupTag == null) continue;

                String uuidStr = groupTag.getString("id").orElse("");

                try {
                    UUID groupId = UUID.fromString(uuidStr);
                    List<BlockPos> positions = new ArrayList<>();

                    groupTag.getList("positions").ifPresent(positionsListTag -> {
                        for (int j = 0; j < positionsListTag.size(); j++) {
                            CompoundTag posTag = positionsListTag.getCompound(j).orElse(null);
                            if (posTag == null) continue;

                            int x = posTag.getInt("x").orElse(0);
                            int y = posTag.getInt("y").orElse(0);
                            int z = posTag.getInt("z").orElse(0);
                            positions.add(new BlockPos(x, y, z));
                        }
                    });

                    data.groups.put(groupId, positions);
                } catch (IllegalArgumentException e) {
                    // UUID 格式错误，跳过
                }
            }
        });

        return data;
    }

    /**
     * 保存到 NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag groupsList = new ListTag();

        for (Map.Entry<UUID, List<BlockPos>> entry : groups.entrySet()) {
            CompoundTag groupTag = new CompoundTag();
            groupTag.putString("id", entry.getKey().toString());

            ListTag positionsList = new ListTag();
            for (BlockPos pos : entry.getValue()) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", pos.getX());
                posTag.putInt("y", pos.getY());
                posTag.putInt("z", pos.getZ());
                positionsList.add(posTag);
            }

            groupTag.put("positions", positionsList);
            groupsList.add(groupTag);
        }

        tag.put("groups", groupsList);
        return tag;
    }

    @Override
    public boolean isDirty() {
        return !groups.isEmpty();
    }

    // ========== 组管理方法 ==========

    /**
     * 获取组的所有成员坐标
     * @param groupId 组 ID
     * @return 成员坐标列表（不可变）
     */
    public List<BlockPos> getGroupMembers(UUID groupId) {
        List<BlockPos> members = groups.get(groupId);
        return members != null ? Collections.unmodifiableList(members) : Collections.emptyList();
    }

    /**
     * 获取组的大小
     * @param groupId 组 ID
     * @return 成员数量
     */
    public int getGroupSize(UUID groupId) {
        List<BlockPos> members = groups.get(groupId);
        return members != null ? members.size() : 0;
    }

    /**
     * 将坐标添加到指定组
     * @param groupId 组 ID
     * @param pos 坐标
     */
    public void addToGroup(UUID groupId, BlockPos pos) {
        groups.computeIfAbsent(groupId, k -> new ArrayList<>()).add(pos);
        setDirty();
    }

    /**
     * 从组中移除坐标，如果导致不连通则自动拆分
     * @param pos 要移除的坐标
     * @return 移除后产生的新组 ID 列表（如果没有拆分则返回空列表）
     */
    public List<UUID> removeFromGroup(BlockPos pos) {
        List<UUID> newGroupIds = new ArrayList<>();
        
        // 查找包含该坐标的组
        UUID groupId = null;
        for (Map.Entry<UUID, List<BlockPos>> entry : groups.entrySet()) {
            if (entry.getValue().contains(pos)) {
                groupId = entry.getKey();
                break;
            }
        }
        
        if (groupId == null) {
            return newGroupIds; // 该坐标不属于任何组
        }
        
        List<BlockPos> members = groups.get(groupId);
        if (members == null) {
            return newGroupIds;
        }
        
        // 移除坐标
        members.remove(pos);
        
        if (members.isEmpty()) {
            // 组内没有成员了，直接删除组
            groups.remove(groupId);
            setDirty();
            return newGroupIds;
        }
        
        // 检查移除后组内成员是否仍然连通
        List<List<BlockPos>> connectedComponents = findConnectedComponents(members);
        
        if (connectedComponents.size() == 1) {
            // 仍然连通，不需要拆分
            setDirty();
        } else {
            // 不连通，需要拆分成多个组
            groups.remove(groupId);
            
            // 为每个连通分量创建新组
            for (List<BlockPos> component : connectedComponents) {
                UUID newGroupId = UUID.randomUUID();
                groups.put(newGroupId, component);
                newGroupIds.add(newGroupId);
            }
            setDirty();
        }
        
        return newGroupIds;
    }

    /**
     * 使用 BFS 查找所有连通分量
     * @param members 组成员列表
     * @return 连通分量列表，每个分量是一个 BlockPos 列表
     */
    public List<List<BlockPos>> findConnectedComponents(List<BlockPos> members) {
        if (members.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<List<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        
        for (BlockPos startPos : members) {
            if (!visited.contains(startPos)) {
                // 从该起点开始 BFS，找到一个连通分量
                List<BlockPos> component = new ArrayList<>();
                Queue<BlockPos> queue = new ArrayDeque<>();
                
                queue.offer(startPos);
                visited.add(startPos);
                
                while (!queue.isEmpty()) {
                    BlockPos current = queue.poll();
                    component.add(current);
                    
                    // 检查 6 个方向的相邻方块
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                // 排除自身和对角线方向（只检查 6 个正方向）
                                if (dx == 0 && dy == 0 && dz == 0) continue;
                                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                                
                                BlockPos neighbor = current.offset(dx, dy, dz);
                                if (members.contains(neighbor) && !visited.contains(neighbor)) {
                                    visited.add(neighbor);
                                    queue.offer(neighbor);
                                }
                            }
                        }
                    }
                }
                
                components.add(component);
            }
        }
        
        return components;
    }

    /**
     * 合并两个组（将 fromGroup 合并到 toGroup）
     * @param toGroupId 目标组 ID
     * @param fromGroupId 源组 ID（会被删除）
     */
    public void mergeGroups(UUID toGroupId, UUID fromGroupId) {
        if (toGroupId.equals(fromGroupId)) {
            return;
        }

        List<BlockPos> fromMembers = groups.remove(fromGroupId);
        if (fromMembers != null) {
            List<BlockPos> toMembers = groups.computeIfAbsent(toGroupId, k -> new ArrayList<>());
            toMembers.addAll(fromMembers);
            setDirty();
        }
    }

    /**
     * 创建新组
     * @param groupId 组 ID
     * @param pos 初始成员坐标
     */
    public void createGroup(UUID groupId, BlockPos pos) {
        List<BlockPos> members = new ArrayList<>();
        members.add(pos);
        groups.put(groupId, members);
        setDirty();
    }

    /**
     * 检查坐标是否属于某个组
     * @param pos 坐标
     * @return 如果坐标属于任何组则返回 true
     */
    public boolean isInGroup(BlockPos pos) {
        for (List<BlockPos> members : groups.values()) {
            if (members.contains(pos)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取指定坐标所属的组 ID
     * @param pos 坐标
     * @return 组 ID，如果不属于任何组则返回 null
     */
    public @Nullable UUID getGroupIdForPosition(BlockPos pos) {
        for (Map.Entry<UUID, List<BlockPos>> entry : groups.entrySet()) {
            if (entry.getValue().contains(pos)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 清除所有数据
     */
    public void clear() {
        groups.clear();
        setDirty();
    }
}
