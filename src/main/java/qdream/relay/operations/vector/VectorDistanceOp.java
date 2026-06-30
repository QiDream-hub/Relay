package qdream.relay.operations.vector;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.NumberType;
import qdream.relay.types.VectorType;

import net.minecraft.world.phys.Vec3;

/**
 * 向量距离计算操作
 * 计算两个点之间的距离
 * 
 * 弹出：vector, vector
 * 压入：number (两点间的欧几里得距离)
 */
public class VectorDistanceOp extends Spell {

    public VectorDistanceOp() {
        super("relay:vector_distance", 1, 2, OperationSignature.builder()
                .consumesFromData("relay:vector")
                .consumesFromData("relay:vector")
                .producesToData("relay:number")
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
        
        double distance = aVec.asVector().distanceTo(bVec.asVector());
        executor.pushData(new NumberType(distance));
    }
}
