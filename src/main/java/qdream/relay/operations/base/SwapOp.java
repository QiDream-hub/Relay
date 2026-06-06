package qdream.relay.operations.base;

import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;

/**
 * Swap 操作 - 交换数据栈顶部两个元素
 */
public class SwapOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable topData = executor.popData();
        if (topData == null) return;
        Executable secondData = executor.popData();
        if (secondData == null) return;
        
        executor.pushData(topData);
        executor.pushData(secondData);
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("any")
                .input("any")
                .output("any")
                .output("any")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
