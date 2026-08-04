package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
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
public class VectorDot extends Instruction {

    public VectorDot() {
        super("relay:vector_dot", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "relay:vector")
                .consumesFromData("right", "relay:vector")
                .producesToData("dotProduct", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData bVec = StackHelpers.popVector(executor, id);
        VectorData aVec = StackHelpers.popVector(executor, id);

        double result = aVec.asVector().dot(bVec.asVector());
        executor.pushData(new NumberData(result));
    }
}
