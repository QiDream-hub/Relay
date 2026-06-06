package qdream.relay.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;

/**
 * 数字类型
 * 执行时自动压入数据栈
 */
public class NumberIota implements Executable {
    private final double value;

    public NumberIota(double value) {
        this.value = value;
    }

    public NumberIota(int value) {
        this.value = value;
    }

    @Override
    public String getId() {
        return "relay:number";
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", "relay:number");
        if (value == (int) value) {
            json.addProperty("value", (int) value);
        } else {
            json.addProperty("value", value);
        }
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public double asDouble() {
        return value;
    }

    public int asInt() {
        return (int) value;
    }

    public boolean isInteger() {
        return value == (int) value;
    }
}
