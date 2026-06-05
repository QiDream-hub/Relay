package qdream.relay.operations.logic;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

/**
 * Not 操作 - 逻辑非
 */
public class NotOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData aData = executor.popData();
        if (!(aData instanceof McIota a)) return;
        
        if (a == null) {
            return;
        }
        
        if (!a.isBoolean()) {
            throw new IllegalArgumentException("Not 需要一个布尔参数");
        }
        
        boolean result = !a.asBoolean();
        executor.pushData(McIota.ofBoolean(result));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("boolean")
                .output("boolean")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
