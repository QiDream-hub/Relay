package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;

/**
 * Div 操作 - 除法
 */
public class DivOp extends Spell {

    public DivOp() {
        super("relay:div", 1, 1, OperationSignature.builder()
                .consumesFromData("dividend", "relay:number")
                .consumesFromData("divisor", "relay:number")
                .producesToData("quotient", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = OperationHelpers.popNumber(executor, id);
        if (b == null) return;
        
        NumberData a = OperationHelpers.popNumber(executor, id);
        if (a == null) return;

        double divisor = b.asDouble();
        if (divisor == 0) {
            executor.triggerMishap("操作 relay:div 除零错误");
            return;
        }

        double result = a.asDouble() / divisor;
        executor.pushData(new NumberData(result));
    }

}
