package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;

/**
 * Mod 操作 - 取余
 */
public class ModOp extends Spell {

    public ModOp() {
        super("relay:mod", 1, 0.25, OperationSignature.builder()
                .consumesFromData("dividend", "relay:number")
                .consumesFromData("divisor", "relay:number")
                .producesToData("remainder", "relay:number")
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
            executor.triggerMishap("操作 relay:mod 除零错误");
            return;
        }

        double result = a.asDouble() % divisor;
        executor.pushData(new NumberData(result));
    }

}
