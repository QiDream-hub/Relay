package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Sub 操作 - 减法
 */
public class SubOp extends Spell {

    public SubOp() {
        super("relay:sub", 1, 0.05, OperationSignature.builder()
                .consumesFromData("minuend", "relay:number")
                .consumesFromData("subtrahend", "relay:number")
                .producesToData("difference", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = StackHelpers.popNumber(executor, id);
        if (b == null) return;
        
        NumberData a = StackHelpers.popNumber(executor, id);
        if (a == null) return;

        double result = a.asDouble() - b.asDouble();
        executor.pushData(new NumberData(result));
    }

}
