package qdream.relay.operations.entity;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.EntityData;
import qdream.relay.types.VectorData;

/**
 * 获取实体眼睛位置操作
 * 获取实体当前的眼睛位置坐标（用于射线追踪等视线相关操作）
 *
 * 弹出：entity
 * 压入：vector (实体眼睛位置坐标)
 *
 * 示例用法：
 * 1. 获取自身眼睛位置：get_self get_entity_eye_pos
 * 2. 视线追踪起点：get_self get_entity_eye_pos get_self get_look_vector 10
 * block_raycast
 */
public class GetEntityEyePos extends Instruction {

    public GetEntityEyePos() {
        super("relay:get_entity_eye_pos", 1, 1, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("eyePosition", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        EntityData popEntity = StackHelpers.popEntity(executor, id);

        var entity = popEntity.getEntity();
        if (entity == null) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ENTITY_REFERENCE_INVALID));
        }

        // 获取实体眼睛位置（用于射线追踪的起点）
        var eyePos = entity.getEyePosition();
        executor.pushData(new VectorData(eyePos));
    }
}
