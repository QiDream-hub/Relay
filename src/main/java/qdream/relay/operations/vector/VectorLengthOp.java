package qdream.relay.operations.vector;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.NumberType;
import qdream.relay.types.VectorType;

import net.minecraft.world.phys.Vec3;

/**
 * 向量长度计算操作
 * 
 * 弹出：vector
 * 压入：number (向量的模长)
 */
public class VectorLengthOp extends Spell {

    public VectorLengthOp() {
        super("relay:vector_length", 1, 1, OperationSignature.builder()
                .consumesFromData("relay:vector")
                .producesToData("relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable vecExe = executor.popData();
        
        if (vecExe == null) {
            executor.triggerMishap("数据栈不足，需要 vector");
            return;
        }
        
        if (!(vecExe instanceof VectorType vec)) {
            executor.triggerMishap("期望 vector 类型");
            return;
        }
        
        double length = vec.asVector().length();
        executor.pushData(new NumberType(length));
    }
}
