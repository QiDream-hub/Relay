package qdream.relay.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;

/**
 * 布尔类型
 * 执行时自动压入数据栈
 */
public class BooleanIota implements Executable {
    private final boolean value;

    public BooleanIota(boolean value) {
        this.value = value;
    }

    @Override
    public String getType() {
        return "relay:boolean";
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "relay:boolean");
        json.addProperty("value", value);
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public boolean asBoolean() {
        return value;
    }
}
