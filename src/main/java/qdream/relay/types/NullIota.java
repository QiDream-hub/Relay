package qdream.relay.types;

import com.google.gson.JsonObject;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Data;

/**
 * 空值类型
 * 执行时自动压入数据栈
 */
public class NullIota extends Data {
    public static final NullIota INSTANCE = new NullIota();

    private NullIota() {
        super("relay:null", 0);
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    @Override
    public Data fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for NullIota: " + id);
        }
        return INSTANCE;
    }

    @Override
    public JsonObject toJson(Data data) {
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        json.add("value", null);
        return json;
    }
}
