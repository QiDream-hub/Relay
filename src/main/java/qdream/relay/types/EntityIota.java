package qdream.relay.types;

import com.google.gson.JsonObject;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

import java.util.UUID;

/**
 * 实体类型
 * 执行时自动压入数据栈
 */
public class EntityIota extends Data {
    private final UUID value;

    public EntityIota(UUID value) {
        super("relay:entity", 0);
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public UUID asEntity() {
        return value;
    }

    @Override
    public Data fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for EntityIota: " + id);
        }
        UUID value = UUID.fromString(json.get("value").getAsString());
        return new EntityIota(value);
    }

    @Override
    public JsonObject toJson(Data data) {
        EntityIota entityData = (EntityIota) data;
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        json.addProperty("value", entityData.value.toString());
        return json;
    }
}
