package qdream.relay.operations.block;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.BlockData;
import qdream.relay.types.VectorData;

/**
 * 从坐标获取方块操作
 * 检测指定位置的方块状态并返回引用
 *
 * 弹出：vector (位置)
 * 压入：block (方块状态引用，如果不存在则为 air)
 *
 * 需要世界交互器，并检查范围
 *
 * 示例用法：
 * 1. 获取方块：some_vector get_block
 * 2. 检查是否是空气：some_vector get_block is_air if ...
 * 3. 获取坐标并存储：get_self get_entity_pos get_block some_list list_append
 */
public class GetBlock extends Instruction {

    public GetBlock() {
        super("relay:get_block", 1, 1, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("block", "relay:block")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        OperationHelpers.checkWorldInteractor(executor, id);

        // 弹出参数
        VectorData pos = StackHelpers.popVector(executor, id);

        Vec3 posVec = pos.asVector();
        BlockPos blockPos = BlockPos.containing(posVec);

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        OperationHelpers.checkInRange(executor, id, sourcePos, posVec);

        // 获取 Level 上下文
        Level level = OperationHelpers.getLevel(executor, id);

        // 获取方块状态
        BlockState blockState = level.getBlockState(blockPos);

        // 创建 BlockType
        executor.pushData(BlockData.from(blockState, blockPos, level));
    }
}
