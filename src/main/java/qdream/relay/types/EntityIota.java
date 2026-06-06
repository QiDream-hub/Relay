package qdream.relay.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;

import java.util.UUID;

/**
 * 实体类型
 * 执行时自动压入数据栈
 */
public class EntityIota implements Executable {
    private final UUID value;

    public EntityIota(UUID value) {
        this.value = value;
    }

    @Override
    public String getId() {
        return "relay:entity";
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", "relay:entity");
        json.addProperty("value", value.toString());
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public UUID asEntity() {
        return value;
    }
}
