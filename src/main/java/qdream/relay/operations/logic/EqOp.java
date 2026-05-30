package qdream.relay.operations.logic;

import qdream.relay.core.Iota;
import qdream.relay.core.OperationSignature;
import qdream.relay.core.IotaType;
import qdream.relay.core.StackOperation;
import qdream.relay.core.StateMachine;

/**
 * Eq 操作 - 等于比较
 */
public class EqOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota b = executor.popData();
        Iota a = executor.popData();
        
        if (b == null || a == null) {
            return;
        }
        
        boolean result = a.equals(b);
        executor.pushData(Iota.ofBoolean(result));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.ANY)
                .input(IotaType.ANY)
                .output(IotaType.BOOLEAN)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
