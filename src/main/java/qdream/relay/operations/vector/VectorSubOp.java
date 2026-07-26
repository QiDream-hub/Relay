package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.VectorData;

import net.minecraft.world.phys.Vec3;

/**
 * 向量减法操作
 *
 * 弹出：vector, vector
 * 压入：vector (a - b)
 */
public class VectorSubOp extends Instruction {

    public VectorSubOp() {
        super("relay:vector_sub", 1, 0.05, OperationSignature.builder()
                .consumesFromData("minuend", "relay:vector")
                .consumesFromData("subtrahend", "relay:vector")
                .producesToData("difference", "relay:vector")
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

        Vec3 result = aVec.asVector().subtract(bVec.asVector());
        executor.pushData(new VectorData(result));
    }
}
