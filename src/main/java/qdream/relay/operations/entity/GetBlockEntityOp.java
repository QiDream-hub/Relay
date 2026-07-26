package qdream.relay.operations.entity;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.BlockEntityData;
import qdream.relay.types.VectorData;

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
public class GetBlockEntityOp extends Instruction {

    public GetBlockEntityOp() {
        super("relay:get_block_entity", 1, 1, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("block_entity", "relay:block_entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        // 弹出参数
        VectorData pos = StackHelpers.popVector(executor, id);
        if (pos == null) return;

        Vec3 posVec = pos.asVector();
        BlockPos blockPos = BlockPos.containing(posVec);

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        if (!OperationHelpers.checkInRange(executor, id, sourcePos, posVec)) {
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty()) return;

        Level level = levelOpt.get();

        // 获取方块实体
        BlockEntity blockEntity = level.getBlockEntity(blockPos);

        // 创建 BlockEntityType
        if (blockEntity != null) {
            executor.pushData(BlockEntityData.from(blockEntity, level));
        } else {
            // 返回 null BlockEntityType
            executor.pushData(new BlockEntityData(blockPos, level.dimension().identifier().toString(), null));
        }
    }
}
