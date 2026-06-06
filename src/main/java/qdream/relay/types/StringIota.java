package qdream.relay.types;

import com.google.gson.JsonObject;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 字符串类型
 * 执行时自动压入数据栈
 */
public class StringIota extends Data {
    private final String value;

    public StringIota(String value) {
        super("relay:string", 0);
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public String asString() {
        return value;
    }

    @Override
    public Data fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for StringIota: " + id);
        }
        String value = json.get("value").getAsString();
        return new StringIota(value);
    }

    @Override
    public JsonObject toJson(Data data) {
        StringIota stringData = (StringIota) data;
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        json.addProperty("value", stringData.value);
        return json;
    }
}
