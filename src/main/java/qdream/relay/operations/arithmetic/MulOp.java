package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;

/**
 * Mul 操作 - 乘法
 */
public class MulOp extends Spell {

    public MulOp() {
        super("relay:mul", 1, 0.25, OperationSignature.builder()
                .consumesFromData("multiplicand", "relay:number")
                .consumesFromData("multiplier", "relay:number")
                .producesToData("product", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = OperationHelpers.popNumber(executor, id);
        if (b == null) return;
        
        NumberData a = OperationHelpers.popNumber(executor, id);
        if (a == null) return;

        double result = a.asDouble() * b.asDouble();
        executor.pushData(new NumberData(result));
    }

}
