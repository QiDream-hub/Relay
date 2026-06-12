package qdream.relay.types;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

import java.util.UUID;

/**
 * 实体类型
 * 执行时自动压入数据栈
 */
public class EntityIota extends Data {
    private final UUID entityId;

    public EntityIota(UUID entityId) {
        super("relay:entity", 0);
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
        tag.putString("value", entityId.toString());
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        String uuidStr = tag.getString("value").orElse("");
        UUID uuid = uuidStr.isEmpty() ? new UUID(0, 0) : UUID.fromString(uuidStr);
        return new EntityIota(uuid);
    }

    @Override
    public void toJson(JsonObject json) {
        json.addProperty("value", entityId.toString());
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value")) {
            String uuidStr = json.get("value").getAsString();
            UUID uuid = uuidStr.isEmpty() ? new UUID(0, 0) : UUID.fromString(uuidStr);
            return new EntityIota(uuid);
        }
        return new EntityIota(new UUID(0, 0));
    }
}
