package qdream.relay.operations.base;

import qdream.relay.core.Iota;
import qdream.relay.core.OperationSignature;
import qdream.relay.core.IotaType;
import qdream.relay.core.StackOperation;
import qdream.relay.core.StateMachine;

/**
 * Swap 操作 - 交换数据栈顶部两个元素
 */
public class SwapOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota top = executor.popData();
        Iota second = executor.popData();
        if (top != null && second != null) {
            executor.pushData(top);
            executor.pushData(second);
        }
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.ANY)
                .input(IotaType.ANY)
                .output(IotaType.ANY)
                .output(IotaType.ANY)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
