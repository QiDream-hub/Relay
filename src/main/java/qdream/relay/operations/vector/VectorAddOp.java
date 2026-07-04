package qdream.relay.operations.vector;

import qdream.relay.operations.base.OperationHelpers;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.VectorData;

import net.minecraft.world.phys.Vec3;

/**
 * 向量加法操作
 *
 * 弹出：vector, vector
 * 压入：vector (两个向量的和)
 */
public class VectorAddOp extends Spell {

    public VectorAddOp() {
        super("relay:vector_add", 1, 2, OperationSignature.builder()
                .consumesFromData("left", "relay:vector")
                .consumesFromData("right", "relay:vector")
                .producesToData("sum", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData bVec = OperationHelpers.popVector(executor, "relay:vector_add");
        if (bVec == null) return;
        
        VectorData aVec = OperationHelpers.popVector(executor, "relay:vector_add");
        if (aVec == null) return;

        Vec3 result = aVec.asVector().add(bVec.asVector());
        executor.pushData(new VectorData(result));
    }
}
