package qdream.relay.operations.control;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

/**
 * Stop 操作 - 强制终止程序
 * 清空程序栈和数据栈
 */
public class StopOp extends AbstractOperation {

    protected StopOp() {
        super("relay:stop", 1, OperationSignature.builder().build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 清空程序栈
        while (executor.getProgramStackSize() > 0) {
            executor.popProgram();
        }
        // 触发事故来清空数据栈
        executor.triggerMishap("stop 操作被调用");
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
            throw new IllegalArgumentException("Invalid ID for StopOp: " + id);
        }
        return new StopOp();
    }
}
