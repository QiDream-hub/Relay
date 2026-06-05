package qdream.relay.operations.base;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

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
                .input("any")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
