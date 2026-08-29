package qdream.relay.operations.vector;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;
import qdream.relay.types.VectorData;

/**
 * 向量推动操作
 * 对指定实体施加动量/推力
 *
 * 弹出：entity (目标实体), vector (推力向量)
 * 压入：boolean (是否成功推动)
 *
 * 需要世界交互器，并检查实体到施法者的距离
 *
 * 能量消耗：基础 1 + 向量模长 × 0.5
 */
public class PushVector extends Instruction {

    public PushVector() {
        super("relay:push_vector", 1, 2, OperationSignature.builder()
                .consumesFromData("targetEntity", "relay:entity")
                .consumesFromData("pushVector", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        OperationHelpers.checkWorldInteractor(executor, id);

        // 获取施法者位置（从 self 上下文）
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);

        // 弹出参数
        EntityData entityExe = StackHelpers.popEntity(executor, id);
        VectorData pushExe = StackHelpers.popVector(executor, id);

        Entity targetEntity = entityExe.getEntity();

        if (targetEntity == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Vec3 pushVector = pushExe.asVector();
        Vec3 targetPos = targetEntity.position();

        // 检查范围：施法者到目标实体的距离
        OperationHelpers.checkInRange(executor, id, sourcePos, targetPos);

        // 动态计算并扣除能量：向量模长 × 系数
        // checkEnergy 会自动加上操作的基础能量消耗
        double dynamicEnergy = pushVector.length() * 0.25;
        OperationHelpers.checkEnergy(executor, id, dynamicEnergy);

        targetEntity.push(pushVector);
        // 强制同步速度到客户端（对玩家有效）
        if (targetEntity instanceof Player) {
            targetEntity.hurtMarked = true;
        }

    }
}
