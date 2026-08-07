package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import qdream.relay.Relay;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.DataSignature;

/**
 * 方块类型
 * 用于在程序中存储对 BlockState 的引用
 *
 * 设计原则：
 * 1. 通过 BlockPos + 世界 ID 唯一标识方块
 * 2. blockStateRef 为运行时缓存，可能为 null（需要时通过世界查询）
 * 3. 序列化存储位置和世界 ID，反序列化时延迟查询
 *
 * 使用方式：
 * 1. 从 BlockState 创建：BlockType.from(blockState, pos, world)
 * 2. 获取方块状态：getBlockState(world) - 通过世界查询位置获取实际状态
 * 3. 执行：execute() - 将自己压入数据栈
 */
public class BlockData extends Data {
    // 方块位置
    private final BlockPos blockPos;

    // 世界 ID 字符串（例如 "minecraft:overworld"）
    private final String worldId;

    // 运行时缓存，不序列化
    private transient BlockState blockStateRef;

    public BlockData(BlockPos blockPos, String worldId, BlockState blockStateRef) {
        super("relay:block", 0, DataSignature.builder()
                .output("relay:block")
                .field("world", "String")
                .field("x", "Number")
                .field("y", "Number")
                .field("z", "Number")
                .build());
        this.blockPos = blockPos;
        this.worldId = worldId;
        this.blockStateRef = blockStateRef;
    }

    /**
     * 从 BlockState 创建 BlockType（存储 BlockPos + 世界 ID + 引用）
     */
    public static BlockData from(BlockState blockState, BlockPos pos, Level world) {
        if (blockState == null) {
            return new BlockData(null, null, null);
        }
        String worldId = world.dimension().identifier().toString();
        BlockPos immutablePos = pos.immutable();
        return new BlockData(immutablePos, worldId, blockState);
    }

    /**
     * 从 BlockPos 和世界 ID 创建 BlockType（用于反序列化，不保持引用）
     */
    public static BlockData fromBlockPos(BlockPos blockPos, String worldId) {
        return new BlockData(blockPos, worldId, null);
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    /**
     * 获取方块状态（通过世界查询位置）
     * 
     * @return 方块状态，如果不存在则返回 null
     */
    public BlockState getBlockState() {
        // 如果有缓存引用，先验证是否仍然有效
        if (blockStateRef != null) {
            return blockStateRef;
        }

        // 缓存失效，通过 BlockPos 查询
        if (blockPos == null || worldId == null) {
            return null;
        }

        return Relay.getWorld(worldId).getBlockState(blockPos);
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
        return blockPos == null && worldId == null && blockStateRef == null;
    }

    /**
     * 更新方块状态引用缓存
     */
    public void refreshCache(BlockState blockState) {
        this.blockStateRef = blockState;
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
            return new BlockData(null, null, null);
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

        return BlockData.fromBlockPos(blockPos, worldId);
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

            return BlockData.fromBlockPos(blockPos, worldId);
        }
        return new BlockData(null, null, null);
    }

    @Override
    public boolean equalsTo(Operation other) {
        if (!(other instanceof BlockData)) {
            return false;
        }
        BlockData that = (BlockData) other;
        if (this.blockPos == null && that.blockPos == null) {
            return true;
        }
        if (this.blockPos == null || that.blockPos == null) {
            return false;
        }
        return this.blockPos.equals(that.blockPos);
    }

    @Override
    public String asString() {
        if (blockPos == null || worldId != null) {
            return "(null,-1,-1,-1)B";
        }
        return String.format("(%s,%d,%d,%d)B",
                worldId != null ? worldId : "null",
                blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    @Override
    public boolean asBoolean() {
        return blockPos != null;
    }
}
