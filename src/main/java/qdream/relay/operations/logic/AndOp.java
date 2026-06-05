package qdream.relay.operations.logic;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

/**
 * And 操作 - 逻辑与
 */
public class AndOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData bData = executor.popData();
        if (!(bData instanceof McIota b)) return;
        IData aData = executor.popData();
        if (!(aData instanceof McIota a)) return;
        
        if (b == null || a == null) {
            return;
        }
        
        if (!a.isBoolean() || !b.isBoolean()) {
            throw new IllegalArgumentException("And 需要两个布尔参数");
        }
        
        boolean result = a.asBoolean() && b.asBoolean();
        executor.pushData(McIota.ofBoolean(result));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("boolean")
                .input("boolean")
                .output("boolean")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
