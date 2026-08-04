package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberData;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Ceil 操作 - 取顶（向上取整）
 */
public class Ceil extends Instruction {

    public Ceil() {
        super("relay:ceil", 0, 0.05, OperationSignature.builder()
                .consumesFromData("value", "relay:number")
                .producesToData("ceil", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData a = StackHelpers.popNumber(executor, id);

        double result = Math.ceil(a.asDouble());
        executor.pushData(new NumberData(result));
    }

}
