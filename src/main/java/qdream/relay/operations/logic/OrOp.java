package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

/**
 * Or 操作 - 逻辑或
 */
public class OrOp extends AbstractOperation {

    protected OrOp() {
        super("relay:or", 1, OperationSignature.builder()
                .input("boolean")
                .input("boolean")
                .output("boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable bData = executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof BooleanIota b)) {
            executor.triggerMishap("操作 relay:or 期望 boolean 类型，实际为：" + bData.getId());
            return;
        }
        Executable aData = executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof BooleanIota a)) {
            executor.triggerMishap("操作 relay:or 期望 boolean 类型，实际为：" + aData.getId());
            return;
        }

        boolean result = a.asBoolean() || b.asBoolean();
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
            throw new IllegalArgumentException("Invalid ID for OrOp: " + id);
        }
        return new OrOp();
    }
}
