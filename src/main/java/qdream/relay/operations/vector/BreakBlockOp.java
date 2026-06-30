package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.WorldInteractorItem;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BooleanType;
import qdream.relay.types.VectorType;

/**
 * 挖掘方块操作
 * 破坏指定位置的方块（基础版本，无附魔）
 *
 * 弹出：vector (方块位置)
 * 压入：boolean (是否成功破坏)
 *
 * 需要世界交互器，并检查范围
 */
public class BreakBlockOp extends Spell {

    public BreakBlockOp() {
        super("relay:break_block", 1, 1, OperationSignature.builder()
                .consumesFromData("relay:vector")
                .producesToData("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!executor.hasContext("worldInteractor")) {
            executor.triggerMishap("break_block 需要世界交互器");
            return;
        }

        Optional<ItemStack> interactorOpt = executor.getContext("worldInteractor", ItemStack.class);
        if (interactorOpt.isEmpty() || interactorOpt.get().isEmpty()) {
            executor.triggerMishap("世界交互器无效");
            return;
        }

        ItemStack interactor = interactorOpt.get();

        // 弹出参数
        Executable posExe = executor.popData();

        if (posExe == null) {
            executor.triggerMishap("数据栈不足，需要 vector");
            return;
        }

        if (!(posExe instanceof VectorType posEx)) {
            executor.triggerMishap("期望 vector 类型");
            return;
        }

        Vec3 posVec = posEx.asVector();
        // 使用 containing 正确处理负数坐标（向下取整而非向零取整）
        BlockPos pos = BlockPos.containing(posVec);

        // 从 self 获取执行者位置作为源位置（self 可能是 Entity 或 BlockEntity）
        Vec3 sourcePos = new Vec3(0, 0, 0);
        var selfOpt = executor.getContext("self", Object.class);
        if (selfOpt.isPresent()) {
            Object self = selfOpt.get();
            if (self instanceof net.minecraft.world.entity.Entity entity) {
                sourcePos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            } else if (self instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
                net.minecraft.core.BlockPos blockPos = blockEntity.getBlockPos();
                sourcePos = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
            }
        }

        // 检查范围
        if (!WorldInteractorItem.isInRange(interactor, sourcePos, posVec)) {
            executor.pushData(new BooleanType(false));
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = executor.getContext("level", Level.class);
        if (levelOpt.isEmpty()) {
            executor.triggerMishap("无法获取世界");
            return;
        }

        Level level = levelOpt.get();

        // 挖掘方块
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            executor.pushData(new BooleanType(false));
            return;
        }

        // 破坏方块（基础版本，无附魔）
        // destroyBlock 参数：位置，是否掉落物品，破坏者实体，更新限制
        boolean destroyed = level.destroyBlock(pos, true, null, 512);
        executor.pushData(new BooleanType(destroyed));
    }
}
