package qdream.relay.operations.arithmetic;

import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.mc.McIotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

/**
 * Sub 操作 - 减法
 */
public class SubOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData bData = executor.popData();
        if (!(bData instanceof McIota b)) return;
        IData aData = executor.popData();
        if (!(aData instanceof McIota a)) return;
        
        if (b == null || a == null) {
            return;
        }
        
        if (!a.isNumber() || !b.isNumber()) {
            throw new IllegalArgumentException("Sub 需要两个数值参数");
        }
        
        double result = a.asDouble() - b.asDouble();
        executor.pushData(McIota.ofDouble(result));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("number")
                .input("number")
                .output("number")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
