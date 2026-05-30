package qdream.relay.operations.logic;

import qdream.relay.core.Iota;
import qdream.relay.core.OperationSignature;
import qdream.relay.core.IotaType;
import qdream.relay.core.StackOperation;
import qdream.relay.core.StateMachine;

/**
 * Not 操作 - 逻辑非
 */
public class NotOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota a = executor.popData();
        
        if (a == null) {
            return;
        }
        
        if (!a.isBoolean()) {
            throw new IllegalArgumentException("Not 需要一个布尔参数");
        }
        
        boolean result = !a.asBoolean();
        executor.pushData(Iota.ofBoolean(result));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.BOOLEAN)
                .output(IotaType.BOOLEAN)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
