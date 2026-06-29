package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.WorldInteractorItem;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BooleanType;
import qdream.relay.types.EntityType;
import qdream.relay.types.VectorType;

/**
 * 向量推动操作
 * 对指定实体施加动量/推力
 * 
 * 弹出：vector (推力向量), entity (目标实体)
 * 压入：boolean (是否成功推动)
 * 
 * 需要世界交互器，并检查实体到施法者的距离
 */
public class PushVectorOp extends Spell {

    public PushVectorOp() {
        super("relay:push_vector", 1, 5, OperationSignature.builder()
                .input("relay:entity")
                .input("relay:vector")
                .output("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!executor.hasContext("worldInteractor")) {
            executor.triggerMishap("push_vector 需要世界交互器");
            return;
        }

        Optional<ItemStack> interactorOpt = executor.getContext("worldInteractor", ItemStack.class);
        if (interactorOpt.isEmpty() || interactorOpt.get().isEmpty()) {
            executor.triggerMishap("世界交互器无效");
            return;
        }

        ItemStack interactor = interactorOpt.get();

        // 获取施法者位置（从 self 上下文）
        Vec3 sourcePos = null;
        Optional<Entity> selfOpt = executor.getContext("self", Entity.class);
        if (selfOpt.isPresent()) {
            sourcePos = selfOpt.get().position();
        }

        if (sourcePos == null) {
            executor.triggerMishap("无法获取施法者位置");
            return;
        }

        // 弹出参数
        Executable entityExe = executor.popData();
        Executable pushExe = executor.popData();

        if (entityExe == null || pushExe == null) {
            executor.triggerMishap("数据栈不足，需要 entity, vector");
            return;
        }

        if (!(entityExe instanceof EntityType entityEx)) {
            executor.triggerMishap("期望 entity 类型");
            return;
        }

        if (!(pushExe instanceof VectorType pushEx)) {
            executor.triggerMishap("期望 vector 类型");
            return;
        }

        Entity targetEntity = entityEx.getEntity();

        if (targetEntity == null) {
            executor.pushData(new BooleanType(false));
            return;
        }

        Vec3 pushVector = pushEx.asVector();
        Vec3 targetPos = targetEntity.position();

        // 检查范围：施法者到目标实体的距离
        if (!WorldInteractorItem.isInRange(interactor, sourcePos, targetPos)) {
            executor.pushData(new BooleanType(false));
            return;
        }

        targetEntity.push(pushVector);
        // 强制同步速度到客户端（对玩家有效）
        if (targetEntity instanceof Player) {
            targetEntity.hurtMarked = true;
        }

        // 传送
        // 计算坐标
        // Vec3 vec3 = targetEntity.position().add(pushVector);
        // targetEntity.stopRiding();
        // targetEntity.teleportTo(vec3.x, vec3.y, vec3.z);

        executor.pushData(new BooleanType(true));
    }
}
