package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 向量距离计算操作
 * 计算两个点之间的距离
 *
 * 弹出：vector, vector
 * 压入：number (两点间的欧几里得距离)
 */
public class VectorDistanceOp extends Spell {

    public VectorDistanceOp() {
        super("relay:vector_distance", 1, 0.05, OperationSignature.builder()
                .consumesFromData("from", "relay:vector")
                .consumesFromData("to", "relay:vector")
                .producesToData("distance", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData bVec = OperationHelpers.popVector(executor, id);
        if (bVec == null)
            return;

        VectorData aVec = OperationHelpers.popVector(executor, id);
        if (aVec == null)
            return;

        double distance = aVec.asVector().distanceTo(bVec.asVector());
        executor.pushData(new NumberData(distance));
    }
}
