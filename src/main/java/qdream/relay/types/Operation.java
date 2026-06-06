package qdream.relay.types;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;

/**
 * 操作引用
 * 可执行单元，执行时会调用操作注册表中的对应操作
 */
public class Operation implements Executable {
    private final String opId;

    public Operation(String opId) {
        this.opId = opId;
    }

    @Override
    public String getType() {
        return "relay:operation";
    }

    @Override
    public Object getValue() {
        return opId;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "relay:operation");
        json.addProperty("op", opId);
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.executeOperation(opId);
    }

    public String getOpId() {
        return opId;
    }
}
