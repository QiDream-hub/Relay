package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;

/**
 * Ceil 操作 - 取顶（向上取整）
 */
public class CeilOp extends Spell {

    public CeilOp() {
        super("relay:ceil", 0, 0.25, OperationSignature.builder()
                .consumesFromData("value", "relay:number")
                .producesToData("ceil", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData a = OperationHelpers.popNumber(executor, id);
        if (a == null) return;

        double result = Math.ceil(a.asDouble());
        executor.pushData(new NumberData(result));
    }

}
