package qdream.relay.operations.vector;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.VectorData;

import net.minecraft.world.phys.Vec3;

/**
 * 向量归一化操作
 * 将向量转换为单位向量（长度为 1，方向不变）
 *
 * 弹出：vector
 * 压入：vector (归一化后的向量)
 */
public class VectorNormalizeOp extends Spell {

    public VectorNormalizeOp() {
        super("relay:vector_normalize", 1, 1, OperationSignature.builder()
                .consumesFromData("vector", "relay:vector")
                .producesToData("normalized", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        VectorData vec = OperationHelpers.popVector(executor, "relay:vector_normalize");
        if (vec == null) return;

        Vec3 v = vec.asVector();
        double length = v.length();

        if (length < 1e-10) {
            executor.triggerMishap("无法归一化零向量");
            return;
        }

        Vec3 result = v.normalize();
        executor.pushData(new VectorData(result));
    }
}
