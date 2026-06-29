package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.EntityType;
import qdream.relay.types.VectorType;

/**
 * 获取实体视线方向操作
 * 获取实体当前的视线方向向量
 * 
 * 弹出：entity
 * 压入：vector (视线方向，已归一化)
 */
public class GetLookVectorOp extends Spell {

    public GetLookVectorOp() {
        super("relay:get_look_vector", 1, 1, OperationSignature.builder()
                .input("relay:entity")
                .output("relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable entityExe = executor.popData();
        
        if (entityExe == null) {
            executor.triggerMishap("数据栈不足，需要 entity");
            return;
        }
        
        if (!(entityExe instanceof EntityType entityEx)) {
            executor.triggerMishap("期望 entity 类型");
            return;
        }
        
        Entity entity = entityEx.getEntity();
        if (entity == null) {
            executor.triggerMishap("实体引用无效");
            return;
        }
        
        // 获取视线方向（使用 yRot 和 xRot 计算）
        float xRot = entity.getXRot();
        float yRot = entity.getYRot();
        
        // 将角度转换为弧度并计算方向向量
        double x = -Math.sin(Math.toRadians(yRot)) * Math.cos(Math.toRadians(xRot));
        double y = -Math.sin(Math.toRadians(xRot));
        double z = Math.cos(Math.toRadians(yRot)) * Math.cos(Math.toRadians(xRot));
        
        Vec3 lookVec = new Vec3(x, y, z).normalize();
        executor.pushData(new VectorType(lookVec));
    }
}
