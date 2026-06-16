package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.signature.DataSignature;
import qdream.relay.mc.signature.SignatureName;

import java.util.UUID;

/**
 * 实体类型
 * 执行时自动压入数据栈
 */
public class EntityIota extends Data {
    private final UUID entityId;

    public EntityIota(UUID entityId) {
        super("relay:entity", 0, DataSignature.builder()
                .output("relay:entity")
                .input(SignatureName.builder().setName("uuid").setType("String").build())
                .build());
        this.entityId = entityId;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public UUID asEntity() {
        return entityId;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);
        CompoundTag valueTag = new CompoundTag();
        valueTag.putString("uuid", entityId.toString());
        tag.put("value", valueTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        CompoundTag valueTag = tag.getCompound("value").orElse(null);
        if (valueTag == null) {
            return new EntityIota(new UUID(0, 0));
        }

        String uuidStr = valueTag.getString("uuid").orElse("");
        UUID uuid = uuidStr.isEmpty() ? new UUID(0, 0) : UUID.fromString(uuidStr);
        return new EntityIota(uuid);
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);
        JsonObject valueObject = new JsonObject();
        valueObject.addProperty("uuid", entityId.toString());
        json.add("value", valueObject);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value")) {
            JsonObject valueObject = json.get("value").getAsJsonObject();
            if (valueObject.has("uuid")) {
                String uuidStr = valueObject.get("uuid").getAsString();
                UUID uuid = uuidStr.isEmpty() ? new UUID(0, 0) : UUID.fromString(uuidStr);
                return new EntityIota(uuid);
            }
        }
        return new EntityIota(new UUID(0, 0));
    }
}