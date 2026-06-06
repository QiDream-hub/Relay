package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

/**
 * Dup 操作 - 复制数据栈顶部
 */
public class DupOp extends AbstractOperation {

    protected DupOp() {
        super("relay:dup", 1, OperationSignature.builder()
                .input("any")
                .output("any")
                .output("any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable topData = executor.popData();
        if (topData != null) {
            executor.pushData(topData);
            executor.pushData(topData);
        }
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
            throw new IllegalArgumentException("Invalid ID for DupOp: " + id);
        }
        return new DupOp();
    }
}
