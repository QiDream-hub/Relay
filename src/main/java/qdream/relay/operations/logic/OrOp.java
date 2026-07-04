package qdream.relay.operations.logic;

import qdream.relay.types.BooleanData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;

/**
 * Or 操作 - 逻辑或
 */
public class OrOp extends Spell {

    public OrOp() {
        super("relay:or",1, 1, OperationSignature.builder()
                .consumesFromData("left", "relay:boolean")
                .consumesFromData("right", "relay:boolean")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        BooleanData b = OperationHelpers.popBoolean(executor, "relay:or");
        if (b == null) return;
        
        BooleanData a = OperationHelpers.popBoolean(executor, "relay:or");
        if (a == null) return;

        boolean result = a.asBoolean() || b.asBoolean();
        executor.pushData(new BooleanData(result));
    }

}
