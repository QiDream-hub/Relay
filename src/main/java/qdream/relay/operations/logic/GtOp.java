package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.types.NumberIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

/**
 * Gt 操作 - 大于比较
 */
public class GtOp extends AbstractOperation {

    protected GtOp() {
        super("relay:gt", 1, OperationSignature.builder()
                .input("number")
                .input("number")
                .output("boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable bData = executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof NumberIota b)) {
            executor.triggerMishap("操作 relay:gt 期望 number 类型，实际为：" + bData.getId());
            return;
        }
        Executable aData = executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof NumberIota a)) {
            executor.triggerMishap("操作 relay:gt 期望 number 类型，实际为：" + aData.getId());
            return;
        }

        boolean result = a.asDouble() > b.asDouble();
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
            throw new IllegalArgumentException("Invalid ID for GtOp: " + id);
        }
        return new GtOp();
    }
}
