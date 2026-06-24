package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;
import qdream.relay.mc.signature.SignatureName;

import java.util.UUID;

/**
 * 实体类型
 * 支持存储 Entity 引用
 * 执行时自动压入数据栈
 *
 * 使用方式：
 * 1. 存储 Entity: EntityIota.from(entity) - 从实体创建（保持引用）
 * 2. 存储 null: EntityIota.from(null) - 创建 null 实体
 * 3. 序列化：toNbt/toJson 存储 UUID
 * 4. 反序列化：fromNbt/fromJson 从 UUID 恢复（需要世界查找）
 */
public class EntityIota extends Data {
    private final UUID entityId;
    private final Entity entityRef;

    private EntityIota(UUID entityId, Entity entityRef) {
        super("relay:entity", 0, DataSignature.builder()
                .output("relay:entity")
                .input(SignatureName.builder().setName("uuid").setType("String").build())
                .build());
        this.entityId = entityId;
        this.entityRef = entityRef;
    }

    /**
     * 从 Entity 创建 EntityIota（保持实体引用）
     * @param entity 实体，可以为 null
     * @return EntityIota 实例
     */
    public static EntityIota from(Entity entity) {
        if (entity == null) {
            return new EntityIota(null, null);
        }
        return new EntityIota(entity.getUUID(), entity);
    }

    /**
     * 从 UUID 创建 EntityIota（用于反序列化，不保持引用）
     * @param entityId 实体 UUID
     * @return EntityIota 实例
     */
    public static EntityIota fromUuid(UUID entityId) {
        return new EntityIota(entityId, null);
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    /**
     * 获取实体 UUID
     * @return 实体 UUID，如果是 null 实体则返回 null
     */
    public UUID asEntity() {
        return entityId;
    }

    /**
     * 获取实体引用（可能为 null）
     * @return 实体引用，如果是反序列化的数据或 entity 已消失则返回 null
     */
    public Entity getEntity() {
        return entityRef;
    }

    /**
     * 是否有有效的实体引用
     */
    public boolean hasEntity() {
        return entityRef != null;
    }

    /**
     * 是否是 null 实体
     */
    public boolean isNull() {
        return entityId == null && entityRef == null;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag valueTag = new CompoundTag();
        if (entityId != null) {
            valueTag.putString("uuid", entityId.toString());
        } else {
            valueTag.putString("uuid", "");
        }
        tag.put("value", valueTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        CompoundTag valueTag = tag.getCompound("value").orElse(null);
        if (valueTag == null) {
            return EntityIota.from(null);
        }

        String uuidStr = valueTag.getString("uuid").orElse("");
        if (uuidStr.isEmpty()) {
            return EntityIota.from(null);
        }

        try {
            UUID uuid = UUID.fromString(uuidStr);
            return EntityIota.fromUuid(uuid);
        } catch (IllegalArgumentException e) {
            return EntityIota.from(null);
        }
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject valueObject = new JsonObject();
        if (entityId != null) {
            valueObject.addProperty("uuid", entityId.toString());
        } else {
            valueObject.addProperty("uuid", "");
        }
        json.add("value", valueObject);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value")) {
            JsonObject valueObject = json.get("value").getAsJsonObject();
            if (valueObject.has("uuid")) {
                String uuidStr = valueObject.get("uuid").getAsString();
                if (uuidStr.isEmpty()) {
                    return EntityIota.from(null);
                }
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    return EntityIota.fromUuid(uuid);
                } catch (IllegalArgumentException e) {
                    return EntityIota.from(null);
                }
            }
        }
        return EntityIota.from(null);
    }
}
