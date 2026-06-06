package qdream.relay.operations.logic;

import qdream.relay.types.BooleanIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;

/**
 * Eq 操作 - 等于比较
 */
public class EqOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable bData = executor.popData();
        if (bData == null) return;
        Executable aData = executor.popData();
        if (aData == null) return;

        boolean result = aData.equals(bData);
        executor.pushData(new BooleanIota(result));
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
