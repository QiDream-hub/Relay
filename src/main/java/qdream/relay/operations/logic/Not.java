package qdream.relay.operations.logic;

import qdream.relay.types.BooleanData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Not 操作 - 逻辑非
 */
public class Not extends Instruction {

    public Not() {
        super("relay:not", 1, 0.05, OperationSignature.builder()
                .consumesFromData("operand", "relay:boolean")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        BooleanData a = StackHelpers.popBoolean(executor, id);
        if (a == null) return;

        boolean result = !a.asBoolean();
        executor.pushData(new BooleanData(result));
    }

}
