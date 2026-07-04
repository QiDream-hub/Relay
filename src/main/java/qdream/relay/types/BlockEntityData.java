package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;

/**
 * 方块实体类型
 * 用于在程序中存储对 BlockEntity 的引用
 * 
 * 设计原则：
 * 1. 通过 BlockPos + 世界 ID 唯一标识方块实体
 * 2. blockEntityRef 为运行时缓存，可能为 null（需要时通过世界查询）
 * 3. 序列化存储位置和世界 ID，反序列化时延迟查询
 * 
 * 使用方式：
 * 1. 从 BlockEntity 创建：BlockEntityIota.from(blockEntity, world)
 * 2. 获取方块实体：getBlockEntity(world) - 通过世界查询位置获取实际引用
 * 3. 执行：execute() - 将自己压入数据栈
 */
public class BlockEntityData extends Data {
    // 方块位置
    private final BlockPos blockPos;

    // 世界 ID 字符串（例如 "minecraft:overworld"）
    private final String worldId;

    // 运行时缓存，不序列化
    private transient BlockEntity blockEntityRef;

    public BlockEntityData(BlockPos blockPos, String worldId, BlockEntity blockEntityRef) {
        super("relay:block_entity", 0, DataSignature.builder()
                .output("relay:block_entity")
                .field("world", "String")
                .field("x", "Number")
                .field("y", "Number")
                .field("z", "Number")
                .build());
        this.blockPos = blockPos;
        this.worldId = worldId;
        this.blockEntityRef = blockEntityRef;
    }

    /**
     * 从 BlockEntity 创建 BlockEntityIota（存储 BlockPos + 世界 ID + 引用）
     */
    public static BlockEntityData from(BlockEntity blockEntity, Level world) {
        if (blockEntity == null) {
            return new BlockEntityData(null, null, null);
        }
        String worldId = blockEntity.getLevel().dimension().registry().toString();
        BlockPos pos = blockEntity.getBlockPos().immutable();
        return new BlockEntityData(pos, worldId, blockEntity);
    }

    /**
     * 从 BlockPos 和世界 ID 创建 BlockEntityIota（用于反序列化，不保持引用）
     */
    public static BlockEntityData fromBlockPos(BlockPos blockPos, String worldId) {
        return new BlockEntityData(blockPos, worldId, null);
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    /**
     * 获取方块实体引用（通过世界查询位置）
     * 
     * @return 方块实体引用，如果不存在则返回 null
     */
    public BlockEntity getBlockEntity(Level world) {
        // 如果有缓存引用，先验证是否仍然有效
        if (blockEntityRef != null && !blockEntityRef.isRemoved()) {
            return blockEntityRef;
        }

        // 缓存失效，通过 BlockPos 查询
        if (blockPos == null || world == null) {
            return null;
        }

        return world.getBlockEntity(blockPos);
    }

    /**
     * 获取方块位置
     * 
     * @return 方块位置
     */
    public BlockPos getBlockPos() {
        return blockPos;
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
     * 是否有有效的位置
     */
    public boolean hasPosition() {
        return blockPos != null;
    }

    /**
     * 是否是 null 引用
     */
    public boolean isNull() {
        return blockPos == null && worldId == null && blockEntityRef == null;
    }

    /**
     * 更新方块实体引用缓存
     */
    public void refreshCache(BlockEntity blockEntity) {
        this.blockEntityRef = blockEntity;
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
        if (blockPos != null) {
            valueTag.putInt("x", blockPos.getX());
            valueTag.putInt("y", blockPos.getY());
            valueTag.putInt("z", blockPos.getZ());
        }
        tag.put("value", valueTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        CompoundTag valueTag = tag.getCompound("value").orElse(null);
        if (valueTag == null) {
            return new BlockEntityData(null, null, null);
        }

        String worldId = null;
        if (valueTag.contains("world")) {
            worldId = valueTag.getString("world").orElse(null);
        }

        BlockPos blockPos = null;
        if (valueTag.contains("x") && valueTag.contains("y") && valueTag.contains("z")) {
            int x = valueTag.getInt("x").orElse(0);
            int y = valueTag.getInt("y").orElse(0);
            int z = valueTag.getInt("z").orElse(0);
            blockPos = new BlockPos(x, y, z);
        }

        return BlockEntityData.fromBlockPos(blockPos, worldId);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject valueObject = new JsonObject();
        if (worldId != null) {
            valueObject.addProperty("world", worldId);
        }

        if (blockPos != null) {
            valueObject.addProperty("x", blockPos.getX());
            valueObject.addProperty("y", blockPos.getY());
            valueObject.addProperty("z", blockPos.getZ());
        }
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

            BlockPos blockPos = null;
            if (valueObject.has("x") && valueObject.has("y") && valueObject.has("z")) {
                int x = valueObject.get("x").getAsInt();
                int y = valueObject.get("y").getAsInt();
                int z = valueObject.get("z").getAsInt();
                blockPos = new BlockPos(x, y, z);
            }

            return BlockEntityData.fromBlockPos(blockPos, worldId);
        }
        return new BlockEntityData(null, null, null);
    }

    @Override
    public String toString() {
        if (blockPos == null && worldId == null) {
            return "BlockEntityData{null}";
        }
        return "BlockEntityData{pos=" + blockPos + ", worldId=" + worldId + "}";
    }
}
