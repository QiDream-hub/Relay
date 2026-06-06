package qdream.relay.operations.base;

import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;

/**
 * Dup 操作 - 复制数据栈顶部
 */
public class DupOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable topData = executor.popData();
        if (topData != null) {
            executor.pushData(topData);
            executor.pushData(topData);
        }
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
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
