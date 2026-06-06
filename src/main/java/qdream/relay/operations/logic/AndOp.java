package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;

/**
 * And 操作 - 逻辑与
 */
public class AndOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable bData = executor.popData();
        if (bData == null) return;
        if (!(bData instanceof BooleanIota b)) {
            executor.triggerMishap("操作 relay:and 期望 boolean 类型，实际为：" + bData.getId());
            return;
        }
        Executable aData = executor.popData();
        if (aData == null) return;
        if (!(aData instanceof BooleanIota a)) {
            executor.triggerMishap("操作 relay:and 期望 boolean 类型，实际为：" + aData.getId());
            return;
        }

        boolean result = a.asBoolean() && b.asBoolean();
        executor.pushData(new BooleanIota(result));
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
