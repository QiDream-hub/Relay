package qdream.relay.operations.logic;

import qdream.relay.types.BooleanData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * And 操作 - 逻辑与
 */
public class And extends Instruction {

    public And() {
        super("relay:and", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "relay:boolean")
                .consumesFromData("right", "relay:boolean")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        BooleanData b = StackHelpers.popBoolean(executor, id);
        BooleanData a = StackHelpers.popBoolean(executor, id);

        boolean result = a.asBoolean() && b.asBoolean();
        executor.pushData(new BooleanData(result));
    }

}
