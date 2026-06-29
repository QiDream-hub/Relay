package qdream.relay.operations.vector;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.NumberType;
import qdream.relay.types.VectorType;

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
                .input("relay:vector")
                .input("relay:vector")
                .output("relay:vector")
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
        
        Vec3 result = aVec.asVector().add(bVec.asVector());
        executor.pushData(new VectorType(result));
    }
}
