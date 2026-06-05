package qdream.relay.operations.logic;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

/**
 * Lt 操作 - 小于比较
 */
public class LtOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota b = executor.popData();
        Iota a = executor.popData();
        
        if (b == null || a == null) {
            return;
        }
        
        if (!a.isNumber() || !b.isNumber()) {
            throw new IllegalArgumentException("Lt 需要两个数值参数");
        }
        
        boolean result = a.asDouble() < b.asDouble();
        executor.pushData(Iota.ofBoolean(result));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.NUMBER)
                .input(IotaType.NUMBER)
                .output(IotaType.BOOLEAN)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
