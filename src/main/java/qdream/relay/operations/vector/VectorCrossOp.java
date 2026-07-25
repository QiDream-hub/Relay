package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.VectorData;

import net.minecraft.world.phys.Vec3;

/**
 * 向量叉积（外积）操作
 *
 * 弹出：vector, vector
 * 压入：vector (垂直于两个输入向量的新向量)
 */
public class VectorCrossOp extends Spell {

    public VectorCrossOp() {
        super("relay:vector_cross", 1, 0.05, OperationSignature.builder()
                .consumesFromData("left", "relay:vector")
                .consumesFromData("right", "relay:vector")
                .producesToData("crossProduct", "relay:vector")
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

        Vec3 av = aVec.asVector();
        Vec3 bv = bVec.asVector();
        Vec3 result = new Vec3(
                av.y * bv.z - av.z * bv.y,
                av.z * bv.x - av.x * bv.z,
                av.x * bv.y - av.y * bv.x);
        executor.pushData(new VectorData(result));
    }
}
