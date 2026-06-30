package qdream.relay.operations.vector;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.NumberType;
import qdream.relay.types.VectorType;

import net.minecraft.world.phys.Vec3;

/**
 * 向量点积（内积）操作
 * 
 * 弹出：vector, vector
 * 压入：number (标量结果)
 */
public class VectorDotOp extends Spell {

    public VectorDotOp() {
        super("relay:vector_dot", 1, 2, OperationSignature.builder()
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
        
        double result = aVec.asVector().dot(bVec.asVector());
        executor.pushData(new NumberType(result));
    }
}
