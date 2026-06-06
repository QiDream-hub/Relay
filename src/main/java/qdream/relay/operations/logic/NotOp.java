package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;

/**
 * Not 操作 - 逻辑非
 */
public class NotOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable aData = executor.popData();
        if (aData == null) return;
        if (!(aData instanceof BooleanIota a)) {
            executor.triggerMishap("操作 relay:not 期望 boolean 类型，实际为：" + aData.getType());
            return;
        }

        boolean result = !a.asBoolean();
        executor.pushData(new BooleanIota(result));
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
