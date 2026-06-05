package qdream.relay.operations.base;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

/**
 * Swap 操作 - 交换数据栈顶部两个元素
 */
public class SwapOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData topData = executor.popData();
        if (!(topData instanceof McIota top)) return;
        IData secondData = executor.popData();
        if (!(secondData instanceof McIota second)) return;
        if (top != null && second != null) {
            executor.pushData(top);
            executor.pushData(second);
        }
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
