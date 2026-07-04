package qdream.relay.operations.entity;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
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
 * 2. 视线追踪起点：get_self get_entity_eye_pos get_self get_look_vector 10 block_raycast
 */
public class GetEntityEyePosOp extends Spell {

    public GetEntityEyePosOp() {
        super("relay:get_entity_eye_pos", 1, 1, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("eyePosition", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable entityExe = executor.popData();

        if (entityExe == null) {
            executor.triggerMishap("数据栈不足，需要 entity");
            return;
        }

        if (!(entityExe instanceof EntityData entityEx)) {
            executor.triggerMishap("期望 entity 类型");
            return;
        }

        var entity = entityEx.getEntity();
        if (entity == null) {
            executor.triggerMishap("实体引用无效");
            return;
        }

        // 获取实体眼睛位置（用于射线追踪的起点）
        var eyePos = entity.getEyePosition();
        executor.pushData(new VectorData(eyePos));
    }
}
