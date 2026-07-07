package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
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
public class PushVectorOp extends Spell {

    public PushVectorOp() {
        super("relay:push_vector", 1, 2, OperationSignature.builder()
                .consumesFromData("target", "relay:entity")
                .consumesFromData("push", "relay:vector")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取施法者位置（从 self 上下文）
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);

        // 弹出参数
        Executable entityExe = executor.popData();
        Executable pushExe = executor.popData();

        if (entityExe == null || pushExe == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        if (!(entityExe instanceof EntityData entityEx)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        if (!(pushExe instanceof VectorData pushEx)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Entity targetEntity = entityEx.getEntity();

        if (targetEntity == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Vec3 pushVector = pushEx.asVector();
        Vec3 targetPos = targetEntity.position();

        // 检查范围：施法者到目标实体的距离
        if (!OperationHelpers.checkInRange(executor, id, sourcePos, targetPos)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 动态计算并扣除能量：基础 + 向量模长 × 系数
        // checkEnergy 会自动加上操作的基础能量消耗
        double dynamicEnergy = pushVector.length() * 0.25;
        if (!OperationHelpers.checkEnergy(executor, id, dynamicEnergy)) {
            return;
        }

        targetEntity.push(pushVector);
        // 强制同步速度到客户端（对玩家有效）
        if (targetEntity instanceof Player) {
            targetEntity.hurtMarked = true;
        }

    }
}
