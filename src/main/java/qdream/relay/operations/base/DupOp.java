package qdream.relay.operations.base;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

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
