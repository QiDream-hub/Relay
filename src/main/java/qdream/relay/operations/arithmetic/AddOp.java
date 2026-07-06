package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;

/**
 * Add 操作 - 加法
 */
public class AddOp extends Spell {

    public AddOp() {
        super("relay:add", 1, 1, OperationSignature.builder()
                .consumesFromData("augend", "relay:number")
                .consumesFromData("addend", "relay:number")
                .producesToData("sum", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = OperationHelpers.popNumber(executor, id);
        if (b == null) return;
        
        NumberData a = OperationHelpers.popNumber(executor, id);
        if (a == null) return;

        double result = a.asDouble() + b.asDouble();
        executor.pushData(new NumberData(result));
    }

}
