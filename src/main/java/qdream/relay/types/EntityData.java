package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import qdream.relay.Relay;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.DataSignature;

import java.util.UUID;

/**
 * 实体类型
 * 支持存储 Entity 引用，通过 UUID + 世界 ID 实现跨维度持久化
 *
 * 设计原则：
 * 1. UUID 唯一标识实体
 * 2. entityRef 为运行时缓存，可能为 null（需要时通过世界查询）
 * 3. 序列化存储 UUID 和世界 ID，反序列化时延迟加载实体
 * 4. getEntity() 时懒加载：首次调用时才查询世界获取实体引用
 *
 * 使用方式：
 * 1. 从实体创建：EntityIota.from(entity, world) - 立即存储 UUID + worldId + 引用
 * 2. 从 UUID 创建：EntityIota.fromUuid(uuid, worldId) - 立即解析实体（需要世界可用）
 * 3. 从 NBT/JSON 创建：fromNbt()/fromJson() - 延迟加载，不立即解析实体
 * 4. 获取实体：getEntity() - 懒加载，首次调用时查询世界
 * 5. 执行：execute() - 将自己压入数据栈
 */
public class EntityData extends Data {
    // 实体 UUID
    private final UUID uuid;

    // 世界 ID 字符串（例如 "minecraft:overworld"）
    private final String worldId;

    // 运行时缓存，不序列化
    private transient Entity entityRef;

    public EntityData(UUID uuid, String worldId, Entity entityRef) {
        super("relay:entity", 0, DataSignature.builder()
                .output("relay:entity")
                .field("world", "String")
                .field("uuid", "String")
                .build());
        this.uuid = uuid;
        this.worldId = worldId;
        this.entityRef = entityRef;
    }

    /**
     * 从 Entity 创建 EntityIota（存储 UUID + 世界 ID + 引用）
     */
    public static EntityData from(Entity entity, Level world) {
        if (entity == null) {
            return new EntityData(null, null, null);
        }
        String worldId = entity.level().dimension().identifier().toString();
        return new EntityData(entity.getUUID(), worldId, entity);
    }

    /**
     * 从 UUID 和世界 ID 创建 EntityIota
     */
    public static EntityData fromUuid(UUID uuid, String worldId) {
        ServerLevel world = Relay.getWorld(worldId);
        Entity entity = world.getEntity(uuid);
        return new EntityData(uuid, worldId, entity);
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    /**
     * 获取实体引用
     *
     * @return 实体引用，如果实体不存在则返回 null
     */
    public Entity getEntity() {
        // 如果有缓存引用，先验证是否仍然有效
        if (entityRef != null && !entityRef.isRemoved()) {
            return entityRef;
        }

        // 延迟加载：通过 UUID 和世界 ID 查询实体
        if (uuid != null && worldId != null) {
            ServerLevel world = Relay.getWorld(worldId);
            if (world != null) {
                entityRef = world.getEntity(uuid);
                return entityRef;
            }
        }

        return null;
    }

    /**
     * 获取实体 UUID
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * 获取世界 ID 字符串
     */
    public String getWorldId() {
        return worldId;
    }

    /**
     * 是否有有效的 UUID
     */
    public boolean hasUuid() {
        return uuid != null;
    }

    /**
     * 是否是 null 引用
     */
    public boolean isNull() {
        return uuid == null && worldId == null && entityRef == null;
    }

    /**
     * 更新实体引用缓存
     */
    public void refreshCache(Entity entity) {
        this.entityRef = entity;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag valueTag = new CompoundTag();
        if (worldId != null) {
            valueTag.putString("world", worldId);
        }

        if (uuid != null) {
            valueTag.putString("uuid", uuid.toString());
        } else {
            valueTag.putString("uuid", "");
        }
        tag.put("value", valueTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        CompoundTag valueTag = tag.getCompound("value").orElse(null);
        if (valueTag == null) {
            return new EntityData(null, null, null);
        }

        String worldId = null;
        if (valueTag.contains("world")) {
            worldId = valueTag.getString("world").orElse(null);
        }

        UUID uuid = null;
        if (valueTag.contains("uuid")) {
            String uuidStr = valueTag.getString("uuid").orElse("");
            if (!uuidStr.isEmpty()) {
                try {
                    uuid = UUID.fromString(uuidStr);
                } catch (IllegalArgumentException e) {
                    // UUID 格式错误，返回 null
                    return new EntityData(null, null, null);
                }
            }
        }

        // 直接创建 EntityIota，不立即解析实体（延迟加载）
        return new EntityData(uuid, worldId, null);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject valueObject = new JsonObject();
        if (worldId != null) {
            valueObject.addProperty("world", worldId);
        }

        if (uuid != null) {
            valueObject.addProperty("uuid", uuid.toString());
        } else {
            valueObject.addProperty("uuid", "");
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

            String uuidStr = valueObject.has("uuid") ? valueObject.get("uuid").getAsString() : "";
            if (uuidStr.isEmpty()) {
                return new EntityData(null, null, null);
            }

            try {
                UUID uuid = UUID.fromString(uuidStr);
                // 直接创建 EntityIota，不立即解析实体（延迟加载）
                return new EntityData(uuid, worldId, null);
            } catch (IllegalArgumentException e) {
                return new EntityData(null, null, null);
            }
        }
        return new EntityData(null, null, null);
    }

    @Override
    public boolean equalsTo(Operation other) {
        if (!(other instanceof EntityData)) {
            return false;
        }
        EntityData that = (EntityData) other;
        if (this.uuid == null && that.uuid == null) {
            return true;
        }
        if (this.uuid == null || that.uuid == null) {
            return false;
        }
        return this.uuid.equals(that.uuid);
    }

    @Override
    public String asString() {
        return String.format("world:%s,uuid:%s",
            worldId != null ? worldId : "null",
            uuid != null ? uuid.toString() : "null");
    }

    @Override
    public boolean asBoolean() {
        return uuid != null;
    }
}
