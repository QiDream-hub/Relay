package qdream.relay.operations.logic;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

/**
 * Eq 操作 - 等于比较
 */
public class EqOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData bData = executor.popData();
        if (!(bData instanceof McIota b)) return;
        IData aData = executor.popData();
        if (!(aData instanceof McIota a)) return;
        
        if (b == null || a == null) {
            return;
        }
        
        boolean result = a.equals(b);
        executor.pushData(McIota.ofBoolean(result));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("any")
                .input("any")
                .output("boolean")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
