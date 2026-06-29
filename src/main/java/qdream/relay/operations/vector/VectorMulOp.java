package qdream.relay.operations.vector;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.NumberType;
import qdream.relay.types.VectorType;

import net.minecraft.world.phys.Vec3;

/**
 * 向量数乘操作
 * 
 * 弹出：number, vector
 * 压入：vector (向量 * 标量)
 */
public class VectorMulOp extends Spell {

    public VectorMulOp() {
        super("relay:vector_mul", 1, 2, OperationSignature.builder()
                .input("relay:number")
                .input("relay:vector")
                .output("relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable vecExe = executor.popData();
        Executable numExe = executor.popData();
        
        if (vecExe == null || numExe == null) {
            executor.triggerMishap("数据栈不足，需要 number 和 vector");
            return;
        }
        
        if (!(vecExe instanceof VectorType vec) || !(numExe instanceof NumberType num)) {
            executor.triggerMishap("期望 vector 和 number 类型");
            return;
        }
        
        double scalar = num.asDouble();
        Vec3 result = vec.asVector().scale(scalar);
        executor.pushData(new VectorType(result));
    }
}
