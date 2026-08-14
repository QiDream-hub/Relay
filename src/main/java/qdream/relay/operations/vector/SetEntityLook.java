package qdream.relay.operations.vector;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.VectorData;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;

/**
 * 设置实体朝向操作
 * 设置实体看向目标方向或目标点
 *
 * 弹出：entity, vector (目标方向或目标点)
 * 压入：无
 *
 * 行为：
 * - vector 会被归一化作为方向向量
 * - 使用 Entity.lookAt() 方法设置实体朝向
 *
 * 验证：
 * - 向量不能为零向量
 * - 向量分量不能为 NaN 或 Infinity
 */
public class SetEntityLook extends Instruction {

    public SetEntityLook() {
        super("relay:set_entity_look", 1, 1, OperationSignature.builder()
                .consumesFromData("direction", "relay:vector")
                .consumesFromData("targetEntity", "relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        OperationHelpers.checkWorldInteractor(executor, id);

        VectorData popVector = StackHelpers.popVector(executor, id);
        EntityData popEntity = StackHelpers.popEntity(executor, id);

        Entity entity = popEntity.getEntity();
        if (entity == null) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_ENTITY_REFERENCE));
        }

        Vec3 direction = popVector.asVector();

        // 验证向量合法性
        if (isInvalidVector(direction)) {
            throw new ParameterException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.INVALID_VECTOR));
        }

        // 检查是否为零向量
        if (direction.lengthSqr() < 1e-10) {
            throw new ParameterException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ZERO_VECTOR));
        }

        direction = direction.normalize();

        // 计算目标点：从实体眼睛位置沿方向向量延伸
        Vec3 eyePos = entity.getEyePosition();
        Vec3 targetPos = eyePos.add(direction);

        // 使用 lookAt 方法设置朝向，自动处理客户端同步
        entity.lookAt(EntityAnchorArgument.Anchor.EYES, targetPos);
    }

    /**
     * 检查向量是否包含非法值（NaN 或 Infinity）
     */
    private boolean isInvalidVector(Vec3 vec) {
        return Double.isNaN(vec.x) || Double.isNaN(vec.y) || Double.isNaN(vec.z) ||
                Double.isInfinite(vec.x) || Double.isInfinite(vec.y) || Double.isInfinite(vec.z);
    }
}
