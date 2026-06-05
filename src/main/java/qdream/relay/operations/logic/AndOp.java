package qdream.relay.operations.logic;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

/**
 * And 操作 - 逻辑与
 */
public class AndOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota b = executor.popData();
        Iota a = executor.popData();
        
        if (b == null || a == null) {
            return;
        }
        
        if (!a.isBoolean() || !b.isBoolean()) {
            throw new IllegalArgumentException("And 需要两个布尔参数");
        }
        
        boolean result = a.asBoolean() && b.asBoolean();
        executor.pushData(Iota.ofBoolean(result));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.BOOLEAN)
                .input(IotaType.BOOLEAN)
                .output(IotaType.BOOLEAN)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
