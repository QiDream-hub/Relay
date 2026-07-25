package qdream.relay.operations.vector;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.VectorData;

/**
 * 获取实体视线方向操作
 * 获取实体当前的视线方向向量
 * 
 * 弹出：entity
 * 压入：vector (视线方向，已归一化)
 */
public class GetLookVectorOp extends Spell {

    public GetLookVectorOp() {
        super("relay:get_look_vector", 1, 0.25, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("lookDirection", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        EntityData popEntity = StackHelpers.popEntity(executor, id);

        Entity entity = popEntity.getEntity();
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
        executor.pushData(new VectorData(lookVec));
    }
}
