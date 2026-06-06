package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

/**
 * Eq 操作 - 等于比较
 */
public class EqOp extends AbstractOperation {

    protected EqOp() {
        super("relay:eq", 1, OperationSignature.builder()
                .input("any")
                .input("any")
                .output("boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable bData = executor.popData();
        if (bData == null)
            return;
        Executable aData = executor.popData();
        if (aData == null)
            return;

        boolean result = aData.equals(bData);
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
            throw new IllegalArgumentException("Invalid ID for EqOp: " + id);
        }
        return new EqOp();
    }
}
