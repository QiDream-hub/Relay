package qdream.relay.types;

import com.google.gson.JsonObject;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 布尔类型
 * 执行时自动压入数据栈
 */
public class BooleanIota extends Data {
    private final boolean value;

    public BooleanIota(boolean value) {
        super("relay:boolean", 0);
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public boolean asBoolean() {
        return value;
    }

    @Override
    public Data fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for BooleanIota: " + id);
        }
        boolean value = json.get("value").getAsBoolean();
        return new BooleanIota(value);
    }

    @Override
    public JsonObject toJson(Data data) {
        BooleanIota booleanData = (BooleanIota) data;
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        json.addProperty("value", booleanData.value);
        return json;
    }
}
