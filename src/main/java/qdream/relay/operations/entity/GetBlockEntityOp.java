package qdream.relay.operations.entity;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.WorldInteractorItem;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BlockEntityType;
import qdream.relay.types.VectorType;

/**
 * 从坐标获取方块实体操作
 * 检测指定位置是否存在方块实体并获取引用
 *
 * 弹出：vector (位置)
 * 压入：block_entity (方块实体引用，如果不存在则为 null)
 *
 * 需要世界交互器，并检查范围
 *
 * 示例用法：
 * 1. 获取方块实体：some_vector get_block_entity
 * 2. 检查是否存在方块实体：some_vector get_block_entity is_null if ...
 * 3. 获取坐标并存储：get_self get_entity_pos get_block_entity some_list list_append
 */
public class GetBlockEntityOp extends Spell {

    public GetBlockEntityOp() {
        super("relay:get_block_entity", 1, 1, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("block_entity", "relay:block_entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器 - 通过 shellContainer 检查
        ShellContainer container = getShellContainer(executor);
        if (container == null || !container.hasWorldInteractor()) {
            executor.triggerMishap("get_block_entity 需要世界交互器");
            return;
        }

        ItemStack interactor = container.getInteractorStack();

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
        net.minecraft.core.BlockPos blockPos = net.minecraft.core.BlockPos.containing(posVec);

        // 从 self 获取执行者位置作为源位置（self 可能是 Entity 或 BlockEntity）
        Vec3 sourcePos = new Vec3(0, 0, 0);
        var selfOpt = executor.getContext("self", Object.class);
        if (selfOpt.isPresent()) {
            Object self = selfOpt.get();
            if (self instanceof net.minecraft.world.entity.Entity entity) {
                sourcePos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            } else if (self instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
                net.minecraft.core.BlockPos blockPosSelf = blockEntity.getBlockPos();
                sourcePos = new Vec3(blockPosSelf.getX() + 0.5, blockPosSelf.getY(), blockPosSelf.getZ() + 0.5);
            }
        }

        // 检查范围
        if (!WorldInteractorItem.isInRange(interactor, sourcePos, posVec)) {
            executor.triggerMishap("get_block_entity 超出世界交互器范围");
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = executor.getContext("level", Level.class);
        if (levelOpt.isEmpty()) {
            executor.triggerMishap("无法获取世界");
            return;
        }

        Level level = levelOpt.get();

        // 获取方块实体
        BlockEntity blockEntity = level.getBlockEntity(blockPos);

        // 创建 BlockEntityType
        if (blockEntity != null) {
            executor.pushData(BlockEntityType.from(blockEntity, level));
        } else {
            // 返回 null BlockEntityType
            executor.pushData(new BlockEntityType(blockPos, level.dimension().registry().toString(), null));
        }
    }

    /**
     * 从上下文获取 ShellContainer
     * @param executor 状态机
     * @return ShellContainer，如果不存在返回 null
     */
    private ShellContainer getShellContainer(StateMachine executor) {
        if (!executor.hasContext("shellContainer")) {
            return null;
        }
        return executor.getContext("shellContainer", ShellContainer.class).orElse(null);
    }
}
