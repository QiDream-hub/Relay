package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

/**
 * Not 操作 - 逻辑非
 */
public class NotOp extends AbstractOperation {

    protected NotOp() {
        super("relay:not", 1, OperationSignature.builder()
                .input("boolean")
                .output("boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable aData = executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof BooleanIota a)) {
            executor.triggerMishap("操作 relay:not 期望 boolean 类型，实际为：" + aData.getId());
            return;
        }

        boolean result = !a.asBoolean();
        executor.pushData(new BooleanIota(result));
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
            throw new IllegalArgumentException("Invalid ID for NotOp: " + id);
        }
        return new NotOp();
    }
}
