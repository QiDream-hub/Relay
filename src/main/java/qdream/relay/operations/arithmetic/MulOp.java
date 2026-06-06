package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberIota;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.operations.AbstractOperation;

import com.google.gson.JsonObject;

import qdream.relay.engine.Executable;

/**
 * Mul 操作 - 乘法
 */
public class MulOp extends AbstractOperation {

    protected MulOp() {
        super("relay:mul", 1, OperationSignature.builder()
                .input("number")
                .input("number")
                .output("number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable bData = executor.popData();
        if (bData == null)
            return;
        if (!(bData instanceof NumberIota b)) {
            executor.triggerMishap("操作 relay:mul 期望 number 类型，实际为：" + bData.getId());
            return;
        }
        Executable aData = executor.popData();
        if (aData == null)
            return;
        if (!(aData instanceof NumberIota a)) {
            executor.triggerMishap("操作 relay:mul 期望 number 类型，实际为：" + aData.getId());
            return;
        }

        double result = a.asDouble() * b.asDouble();
        executor.pushData(new NumberIota(result));
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
            throw new IllegalArgumentException("Invalid ID for MulOp: " + id);
        }
        return new MulOp();
    }
}
