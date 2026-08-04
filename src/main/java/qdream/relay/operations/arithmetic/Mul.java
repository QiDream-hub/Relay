package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Mul 操作 - 乘法
 */
public class Mul extends Instruction {

    public Mul() {
        super("relay:mul", 1, 0.05, OperationSignature.builder()
                .consumesFromData("multiplicand", "relay:number")
                .consumesFromData("multiplier", "relay:number")
                .producesToData("product", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData b = StackHelpers.popNumber(executor, id);
        NumberData a = StackHelpers.popNumber(executor, id);

        double result = a.asDouble() * b.asDouble();
        executor.pushData(new NumberData(result));
    }

}
