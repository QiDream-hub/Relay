package qdream.relay.operations.logic;

import qdream.relay.types.BooleanData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Or 操作 - 逻辑或
 */
public class OrOp extends Instruction {

    public OrOp() {
        super("relay:or", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "relay:boolean")
                .consumesFromData("right", "relay:boolean")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        BooleanData b = StackHelpers.popBoolean(executor, id);
        if (b == null) return;
        
        BooleanData a = StackHelpers.popBoolean(executor, id);
        if (a == null) return;

        boolean result = a.asBoolean() || b.asBoolean();
        executor.pushData(new BooleanData(result));
    }

}
