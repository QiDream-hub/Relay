package qdream.relay.operations.logic;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

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
