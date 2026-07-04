package qdream.relay.operations.arithmetic;

import qdream.relay.operations.base.OperationHelpers;
import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Sub 操作 - 减法
 */
public class SubOp extends Spell {

    public SubOp() {
        super("relay:sub", 1, 1, OperationSignature.builder()
                .consumesFromData("minuend", "relay:number")
                .consumesFromData("subtrahend", "relay:number")
                .producesToData("difference", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = OperationHelpers.popNumber(executor, "relay:sub");
        if (b == null) return;
        
        NumberData a = OperationHelpers.popNumber(executor, "relay:sub");
        if (a == null) return;

        double result = a.asDouble() - b.asDouble();
        executor.pushData(new NumberData(result));
    }

}
