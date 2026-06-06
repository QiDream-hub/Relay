package qdream.relay.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;

/**
 * 字符串类型
 * 执行时自动压入数据栈
 */
public class StringIota implements Executable {
    private final String value;

    public StringIota(String value) {
        this.value = value;
    }

    @Override
    public String getType() {
        return "relay:string";
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "relay:string");
        json.addProperty("value", value);
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public String asString() {
        return value;
    }
}
