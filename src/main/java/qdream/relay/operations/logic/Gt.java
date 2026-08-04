package qdream.relay.operations.logic;

import qdream.relay.types.BooleanData;
import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Gt 操作 - 大于比较
 */
public class Gt extends Instruction {

    public Gt() {
        super("relay:gt", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "relay:number")
                .consumesFromData("right", "relay:number")
                .producesToData("result", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = StackHelpers.popNumber(executor, id);
        NumberData a = StackHelpers.popNumber(executor, id);

        boolean result = a.asDouble() > b.asDouble();
        executor.pushData(new BooleanData(result));
    }

}
