package qdream.relay.operations.logic;

import qdream.relay.types.BooleanData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;

/**
 * And 操作 - 逻辑与
 */
public class AndOp extends Spell {

    public AndOp() {
        super("relay:and", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "relay:boolean")
                .consumesFromData("right", "relay:boolean")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        BooleanData b = OperationHelpers.popBoolean(executor, id);
        if (b == null) return;
        
        BooleanData a = OperationHelpers.popBoolean(executor, id);
        if (a == null) return;

        boolean result = a.asBoolean() && b.asBoolean();
        executor.pushData(new BooleanData(result));
    }

}
