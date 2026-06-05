package qdream.relay.operations.base;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

/**
 * Pop 操作 - 弹出数据栈顶部
 */
public class PopOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        executor.popData();
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.ANY)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
