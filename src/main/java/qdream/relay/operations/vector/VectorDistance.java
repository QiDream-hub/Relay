package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 向量距离计算操作
 * 计算两个点之间的距离
 *
 * 弹出：vector, vector
 * 压入：number (两点间的欧几里得距离)
 */
public class VectorDistance extends Instruction {

    public VectorDistance() {
        super("relay:vector_distance", 1, 0.05, OperationSignature.builder()
                .consumesFromData("from", "relay:vector")
                .consumesFromData("to", "relay:vector")
                .producesToData("distance", "relay:number")
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

        double distance = aVec.asVector().distanceTo(bVec.asVector());
        executor.pushData(new NumberData(distance));
    }
}
