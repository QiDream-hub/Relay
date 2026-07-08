package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;


/**
 * 向量长度计算操作
 *
 * 弹出：vector
 * 压入：number (向量的模长)
 */
public class VectorLengthOp extends Spell {

    public VectorLengthOp() {
        super("relay:vector_length", 1, 0.05, OperationSignature.builder()
                .consumesFromData("vector", "relay:vector")
                .producesToData("length", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData vec = OperationHelpers.popVector(executor, id);
        if (vec == null)
            return;

        double length = vec.asVector().length();
        executor.pushData(new NumberData(length));
    }
}
