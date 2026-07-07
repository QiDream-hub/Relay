package qdream.relay.operations.vector;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.VectorData;

/**
 * 设置实体朝向操作
 * 设置实体看向目标方向或目标点
 *
 * 弹出：entity, vector (目标方向或目标点)
 * 压入：无
 *
 * 行为：
 * - vector 会被归一化作为方向向量
 * - 设置实体的 yRot (水平旋转) 和 xRot (垂直旋转)
 *
 * 验证：
 * - 向量不能为零向量
 * - 向量分量不能为 NaN 或 Infinity
 */
public class SetEntityLookOp extends Spell {

    public SetEntityLookOp() {
        super("relay:set_entity_look", 1, 1, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .consumesFromData("direction", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        EntityData popEntity = OperationHelpers.popEntity(executor, id);
        VectorData popVector = OperationHelpers.popVector(executor, id);

        Entity entity = popEntity.getEntity();
        if (entity == null) {
            executor.triggerMishap(id+" 错误的实体");
            return;
        }

        Vec3 direction = popVector.asVector();

        // 验证向量合法性
        if (isInvalidVector(direction)) {
            executor.triggerMishap("无效的朝向向量（NaN 或 Infinity）");
            return;
        }

        // 检查是否为零向量
        if (direction.lengthSqr() < 1e-10) {
            executor.triggerMishap("朝向向量不能为零向量");
            return;
        }

        direction = direction.normalize();

        // 计算 yaw (yRot) 和 pitch (xRot)
        double x = direction.x;
        double y = direction.y;
        double z = direction.z;

        // yaw: 水平旋转角度，从 -z 轴开始顺时针
        double yaw = Math.toDegrees(Math.atan2(-x, z));

        // pitch: 垂直旋转角度，向上为正
        double horizontalDist = Math.sqrt(x * x + z * z);
        double pitch = Math.toDegrees(Math.atan2(y, horizontalDist));

        entity.setYRot((float) yaw);
        entity.setXRot((float) pitch);
    }

    /**
     * 检查向量是否包含非法值（NaN 或 Infinity）
     */
    private boolean isInvalidVector(Vec3 vec) {
        return Double.isNaN(vec.x) || Double.isNaN(vec.y) || Double.isNaN(vec.z) ||
                Double.isInfinite(vec.x) || Double.isInfinite(vec.y) || Double.isInfinite(vec.z);
    }
}
