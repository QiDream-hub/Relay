package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 向量点积（内积）操作
 *
 * 弹出：vector, vector
 * 压入：number (标量结果)
 */
public class VectorDotOp extends Spell {

    public VectorDotOp() {
        super("relay:vector_dot", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "relay:vector")
                .consumesFromData("right", "relay:vector")
                .producesToData("dotProduct", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData bVec = StackHelpers.popVector(executor, id);
        if (bVec == null)
            return;

        VectorData aVec = StackHelpers.popVector(executor, id);
        if (aVec == null)
            return;

        double result = aVec.asVector().dot(bVec.asVector());
        executor.pushData(new NumberData(result));
    }
}
