package qdream.relay.operations.arithmetic;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

/**
 * Div 操作 - 除法
 */
public class DivOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota b = executor.popData();
        Iota a = executor.popData();
        
        if (b == null || a == null) {
            return;
        }
        
        if (!a.isNumber() || !b.isNumber()) {
            throw new IllegalArgumentException("Div 需要两个数值参数");
        }
        
        double divisor = b.asDouble();
        if (divisor == 0) {
            throw new ArithmeticException("除零错误");
        }
        
        double result = a.asDouble() / divisor;
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
