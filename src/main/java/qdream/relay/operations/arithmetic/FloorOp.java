package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Floor 操作 - 取底（向下取整）
 */
public class FloorOp extends Spell {

    public FloorOp() {
        super("relay:floor", 1, 0.05, OperationSignature.builder()
                .consumesFromData("value", "relay:number")
                .producesToData("floor", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData a = StackHelpers.popNumber(executor, id);
        if (a == null) return;

        double result = Math.floor(a.asDouble());
        executor.pushData(new NumberData(result));
    }

}
