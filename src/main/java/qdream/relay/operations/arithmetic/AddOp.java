package qdream.relay.operations.arithmetic;

import qdream.relay.core.Iota;
import qdream.relay.core.OperationSignature;
import qdream.relay.core.IotaType;
import qdream.relay.core.StackOperation;
import qdream.relay.core.StateMachine;

/**
 * Add 操作 - 加法
 */
public class AddOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota b = executor.popData();
        Iota a = executor.popData();
        
        if (b == null || a == null) {
            return;
        }
        
        if (!a.isNumber() || !b.isNumber()) {
            throw new IllegalArgumentException("Add 需要两个数值参数");
        }
        
        double result = a.asDouble() + b.asDouble();
        executor.pushData(Iota.ofDouble(result));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.NUMBER)
                .input(IotaType.NUMBER)
                .output(IotaType.NUMBER)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
