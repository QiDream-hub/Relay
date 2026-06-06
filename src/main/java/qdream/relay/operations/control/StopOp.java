package qdream.relay.operations.control;

import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;

/**
 * Stop 操作 - 强制终止程序
 * 清空程序栈和数据栈
 */
public class StopOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        // 清空程序栈
        while (executor.getProgramStackSize() > 0) {
            executor.popProgram();
        }
        // 触发事故来清空数据栈
        executor.triggerMishap("stop 操作被调用");
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder().build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
