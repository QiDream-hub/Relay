package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

/**
 * Pop 操作 - 弹出数据栈顶部
 */
public class PopOp extends AbstractOperation {

    protected PopOp() {
        super("relay:pop", 1, OperationSignature.builder()
                .input("any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        executor.popData();
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        return json;
    }

    @Override
    public Executable fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!this.getId().equals(id)) {
            throw new IllegalArgumentException("Invalid ID for PopOp: " + id);
        }
        return new PopOp();
    }
}
