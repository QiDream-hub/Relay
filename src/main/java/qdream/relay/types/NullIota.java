package qdream.relay.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;

/**
 * 空值类型
 * 执行时自动压入数据栈
 */
public class NullIota implements Executable {
    public static final NullIota INSTANCE = new NullIota();

    private NullIota() {
    }

    @Override
    public String getId() {
        return "relay:null";
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", "relay:null");
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }
}
