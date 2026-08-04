package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Floor 操作 - 取底（向下取整）
 */
public class Floor extends Instruction {

    public Floor() {
        super("relay:floor", 1, 0.05, OperationSignature.builder()
                .consumesFromData("value", "relay:number")
                .producesToData("floor", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData a = StackHelpers.popNumber(executor, id);

        double result = Math.floor(a.asDouble());
        executor.pushData(new NumberData(result));
    }

}
