package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Add 操作 - 加法
 */
public class AddOp extends Instruction {

    public AddOp() {
        super("relay:add", 1, 0.05, OperationSignature.builder()
                .consumesFromData("augend", "relay:number")
                .consumesFromData("addend", "relay:number")
                .producesToData("sum", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = StackHelpers.popNumber(executor, id);
        if (b == null) return;
        
        NumberData a = StackHelpers.popNumber(executor, id);
        if (a == null) return;

        double result = a.asDouble() + b.asDouble();
        executor.pushData(new NumberData(result));
    }

}
