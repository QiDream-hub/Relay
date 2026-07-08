package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.DataSignature;

/**
 * 物品引用类型
 * 通过容器坐标（维度 + 位置）和格子索引来引用物品
 *
 * <h3>设计原则</h3>
 * <ul>
 * <li>通过 BlockPos + 世界 ID + 格子索引唯一标识容器中的物品</li>
 * <li>itemStackRef 为运行时缓存，可能为 null（需要时通过世界查询）</li>
 * <li>序列化存储位置、世界 ID 和格子索引，反序列化时延迟查询</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <ul>
 * <li>从 Container 创建：{@link #from(ItemStack, BlockPos, Level, int)}</li>
 * <li>获取物品堆：{@link #getItemStack(Level)} - 通过世界查询位置获取实际物品</li>
 * <li>执行：{@link #execute(StateMachine)} - 将自己压入数据栈</li>
 * </ul>
 */
public class ItemData extends Data {
    /** 容器位置 */
    private final BlockPos containerPos;

    /** 世界 ID 字符串（例如 "minecraft:overworld"） */
    private final String worldId;

    /** 容器格子索引 */
    private final int slot;

    /** 运行时缓存，不序列化 */
    private transient ItemStack itemStackRef;

    public ItemData(BlockPos containerPos, String worldId, int slot, ItemStack itemStackRef) {
        super("relay:item", 0, DataSignature.builder()
                .output("relay:item")
                .field("world", "String")
                .field("x", "Number")
                .field("y", "Number")
                .field("z", "Number")
                .field("slot", "Number")
                .build());
        this.containerPos = containerPos;
        this.worldId = worldId;
        this.slot = slot;
        this.itemStackRef = itemStackRef;
    }

    /**
     * 从 ItemStack 创建 ItemData（存储位置 + 世界 ID + 格子索引 + 引用）
     *
     * @param itemStack 物品堆
     * @param containerPos 容器位置
     * @param world 世界
     * @param slot 格子索引
     * @return ItemData 实例
     */
    public static ItemData from(ItemStack itemStack, BlockPos containerPos, Level world, int slot) {
        if (itemStack == null || itemStack.isEmpty()) {
            return new ItemData(null, null, -1, null);
        }
        String worldId = world.dimension().registry().toString();
        BlockPos immutablePos = containerPos.immutable();
        return new ItemData(immutablePos, worldId, slot, itemStack);
    }

    /**
     * 从坐标和格子索引创建 ItemData（用于反序列化，不保持引用）
     *
     * @param containerPos 容器位置
     * @param worldId 世界 ID
     * @param slot 格子索引
     * @return ItemData 实例
     */
    public static ItemData fromContainer(BlockPos containerPos, String worldId, int slot) {
        return new ItemData(containerPos, worldId, slot, null);
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    /**
     * 获取物品堆（通过世界查询位置）
     *
     * @param world 世界
     * @return 物品堆，如果容器不存在或格子为空返回 EMPTY
     */
    public ItemStack getItemStack(Level world) {
        // 如果有缓存引用，先验证是否仍然有效
        if (itemStackRef != null && !itemStackRef.isEmpty()) {
            return itemStackRef;
        }

        // 缓存失效，通过 BlockPos 查询容器
        if (containerPos == null || world == null || slot < 0) {
            return ItemStack.EMPTY;
        }

        var blockEntity = world.getBlockEntity(containerPos);
        if (blockEntity instanceof Container container) {
            if (slot < container.getContainerSize()) {
                return container.getItem(slot);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取容器位置
     *
     * @return 容器位置
     */
    public BlockPos getContainerPos() {
        return containerPos;
    }

    /**
     * 获取世界 ID 字符串
     *
     * @return 世界 ID 字符串
     */
    public String getWorldId() {
        return worldId;
    }

    /**
     * 获取格子索引
     *
     * @return 格子索引
     */
    public int getSlot() {
        return slot;
    }

    /**
     * 是否有有效的位置
     */
    public boolean hasPosition() {
        return containerPos != null;
    }

    /**
     * 是否是 null 引用
     */
    public boolean isNull() {
        return containerPos == null && worldId == null && slot < 0 && itemStackRef == null;
    }

    /**
     * 更新物品堆引用缓存
     */
    public void refreshCache(ItemStack itemStack) {
        this.itemStackRef = itemStack;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag valueTag = new CompoundTag();
        if (worldId != null) {
            valueTag.putString("world", worldId);
        }

        valueTag.putInt("x", 0);
        valueTag.putInt("y", 0);
        valueTag.putInt("z", 0);
        valueTag.putInt("slot", -1);
        if (containerPos != null) {
            valueTag.putInt("x", containerPos.getX());
            valueTag.putInt("y", containerPos.getY());
            valueTag.putInt("z", containerPos.getZ());
        }
        if (slot >= 0) {
            valueTag.putInt("slot", slot);
        }
        tag.put("value", valueTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        CompoundTag valueTag = tag.getCompound("value").orElse(null);
        if (valueTag == null) {
            return new ItemData(null, null, -1, null);
        }

        String worldId = null;
        if (valueTag.contains("world")) {
            worldId = valueTag.getString("world").orElse(null);
        }

        BlockPos containerPos = null;
        if (valueTag.contains("x") && valueTag.contains("y") && valueTag.contains("z")) {
            int x = valueTag.getInt("x").orElse(0);
            int y = valueTag.getInt("y").orElse(0);
            int z = valueTag.getInt("z").orElse(0);
            containerPos = new BlockPos(x, y, z);
        }

        int slot = -1;
        if (valueTag.contains("slot")) {
            slot = valueTag.getInt("slot").orElse(-1);
        }

        return ItemData.fromContainer(containerPos, worldId, slot);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject valueObject = new JsonObject();
        if (worldId != null) {
            valueObject.addProperty("world", worldId);
        }

        if (containerPos != null) {
            valueObject.addProperty("x", containerPos.getX());
            valueObject.addProperty("y", containerPos.getY());
            valueObject.addProperty("z", containerPos.getZ());
        }
        valueObject.addProperty("slot", slot);
        json.add("value", valueObject);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value")) {
            JsonObject valueObject = json.get("value").getAsJsonObject();

            String worldId = null;
            if (valueObject.has("world")) {
                worldId = valueObject.get("world").getAsString();
            }

            BlockPos containerPos = null;
            if (valueObject.has("x") && valueObject.has("y") && valueObject.has("z")) {
                int x = valueObject.get("x").getAsInt();
                int y = valueObject.get("y").getAsInt();
                int z = valueObject.get("z").getAsInt();
                containerPos = new BlockPos(x, y, z);
            }

            int slot = valueObject.has("slot") ? valueObject.get("slot").getAsInt() : -1;

            return ItemData.fromContainer(containerPos, worldId, slot);
        }
        return new ItemData(null, null, -1, null);
    }

    @Override
    public boolean equalsTo(Operation other) {
        if (!(other instanceof ItemData that)) {
            return false;
        }
        if (this.slot != that.slot) {
            return false;
        }
        if (this.containerPos == null && that.containerPos == null) {
            return true;
        }
        if (this.containerPos == null || that.containerPos == null) {
            return false;
        }
        return this.containerPos.equals(that.containerPos);
    }

    @Override
    public String asString() {
        if (containerPos == null) {
            return String.format("{world:%s,slot:%d}", worldId != null ? worldId : "null", slot);
        }
        return String.format("{world:%s,x:%d,y:%d,z:%d,slot:%d}",
                worldId != null ? worldId : "null",
                containerPos.getX(), containerPos.getY(), containerPos.getZ(), slot);
    }
}
