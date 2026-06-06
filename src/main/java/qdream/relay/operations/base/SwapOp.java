package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

/**
 * Swap 操作 - 交换数据栈顶部两个元素
 */
public class SwapOp extends AbstractOperation {

    protected SwapOp() {
        super("relay:swap", 1, OperationSignature.builder()
                .input("any")
                .input("any")
                .output("any")
                .output("any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable topData = executor.popData();
        if (topData == null)
            return;
        Executable secondData = executor.popData();
        if (secondData == null)
            return;

        executor.pushData(topData);
        executor.pushData(secondData);
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
            throw new IllegalArgumentException("Invalid ID for SwapOp: " + id);
        }
        return new SwapOp();
    }
}
