package qdream.relay.operations.arithmetic;

import qdream.relay.types.NumberIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;

/**
 * Add 操作 - 加法
 */
public class AddOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable bData = executor.popData();
        if (bData == null) return;
        if (!(bData instanceof NumberIota b)) {
            executor.triggerMishap("操作 relay:add 期望 number 类型，实际为：" + bData.getId());
            return;
        }
        Executable aData = executor.popData();
        if (aData == null) return;
        if (!(aData instanceof NumberIota a)) {
            executor.triggerMishap("操作 relay:add 期望 number 类型，实际为：" + aData.getId());
            return;
        }

        double result = a.asDouble() + b.asDouble();
        executor.pushData(new NumberIota(result));
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
