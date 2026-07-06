package qdream.relay.operations.logic;

import qdream.relay.types.BooleanData;
import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;

/**
 * Lt 操作 - 小于比较
 */
public class LtOp extends Spell {

    public LtOp() {
        super("relay:lt", 1, 0.25, OperationSignature.builder()
                .consumesFromData("left", "relay:number")
                .consumesFromData("right", "relay:number")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = OperationHelpers.popNumber(executor, id);
        if (b == null) return;
        
        NumberData a = OperationHelpers.popNumber(executor, id);
        if (a == null) return;

        boolean result = a.asDouble() < b.asDouble();
        executor.pushData(new BooleanData(result));
    }

}
