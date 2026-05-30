package qdream.relay.operations.base;

import qdream.relay.core.Iota;
import qdream.relay.core.OperationSignature;
import qdream.relay.core.IotaType;
import qdream.relay.core.StackOperation;
import qdream.relay.core.StateMachine;

/**
 * Dup 操作 - 复制数据栈顶部
 */
public class DupOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota top = executor.popData();
        if (top != null) {
            executor.pushData(top);
            executor.pushData(top);
        }
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
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
