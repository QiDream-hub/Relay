package qdream.relay.operations.vector;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.VectorType;

import net.minecraft.world.phys.Vec3;

/**
 * 向量叉积（外积）操作
 * 
 * 弹出：vector, vector
 * 压入：vector (垂直于两个输入向量的新向量)
 */
public class VectorCrossOp extends Spell {

    public VectorCrossOp() {
        super("relay:vector_cross", 1, 2, OperationSignature.builder()
                .consumesFromData("left", "relay:vector")
                .consumesFromData("right", "relay:vector")
                .producesToData("crossProduct", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable bExe = executor.popData();
        Executable aExe = executor.popData();
        
        if (bExe == null || aExe == null) {
            executor.triggerMishap("数据栈不足，需要 2 个 vector");
            return;
        }
        
        if (!(bExe instanceof VectorType bVec) || !(aExe instanceof VectorType aVec)) {
            executor.triggerMishap("期望 vector 类型");
            return;
        }
        
        Vec3 av = aVec.asVector();
        Vec3 bv = bVec.asVector();
        Vec3 result = new Vec3(
            av.y * bv.z - av.z * bv.y,
            av.z * bv.x - av.x * bv.z,
            av.x * bv.y - av.y * bv.x
        );
        executor.pushData(new VectorType(result));
    }
}
