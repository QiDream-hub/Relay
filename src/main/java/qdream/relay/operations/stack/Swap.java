package qdream.relay.operations.stack;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;

/**
 * Swap 操作 - 交换数据栈顶部两个元素
 */
public class Swap extends Instruction {

    public Swap() {
        super("relay:swap", 1, 0.25, OperationSignature.builder()
                .consumesFromData("first", "any")
                .consumesFromData("second", "any")
                .producesToData("second", "any")
                .producesToData("first", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable topData = StackHelpers.popAny(executor);
        Executable secondData = StackHelpers.popAny(executor);

        executor.pushData(topData);
        executor.pushData(secondData);
    }

}
