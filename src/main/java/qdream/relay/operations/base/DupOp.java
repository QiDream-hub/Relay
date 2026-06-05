package qdream.relay.operations.base;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

/**
 * Dup 操作 - 复制数据栈顶部
 */
public class DupOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData topData = executor.popData();
        if (!(topData instanceof McIota top)) return;
        if (top != null) {
            executor.pushData(top);
            executor.pushData(top);
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
