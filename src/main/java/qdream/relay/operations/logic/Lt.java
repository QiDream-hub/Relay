package qdream.relay.operations.logic;

import qdream.relay.types.BooleanData;
import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Lt 操作 - 小于比较
 */
public class Lt extends Instruction {

    public Lt() {
        super("relay:lt", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "relay:number")
                .consumesFromData("right", "relay:number")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = StackHelpers.popNumber(executor, id);
        NumberData a = StackHelpers.popNumber(executor, id);

        boolean result = a.asDouble() < b.asDouble();
        executor.pushData(new BooleanData(result));
    }

}
