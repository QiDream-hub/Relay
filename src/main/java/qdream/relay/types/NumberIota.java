package qdream.relay.types;

import com.google.gson.JsonObject;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 数字类型
 * 执行时自动压入数据栈
 */
public class NumberIota extends Data {
    private final double value;

    public NumberIota(double value) {
        super("relay:number", 0);
        this.value = value;
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

    public double getValue() {
        return value;
    }

    public boolean isInteger() {
        return value == (int) value;
    }

    @Override
    public Data fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for NumberIota: " + id);
        }
        double value = json.get("value").getAsDouble();
        return new NumberIota(value);
    }

    @Override
    public JsonObject toJson(Data data) {
        NumberIota numberData = (NumberIota) data;
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        json.addProperty("value", numberData.value);
        return json;
    }
}