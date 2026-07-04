package qdream.relay.operations.entity;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
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
public class GetBlockOp extends Spell {

    public GetBlockOp() {
        super("relay:get_block", 1, 1, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("block", "relay:block")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, "get_block")) {
            return;
        }

        // 弹出参数
        VectorData pos = OperationHelpers.popVector(executor, "get_block");
        if (pos == null) return;

        Vec3 posVec = pos.asVector();
        BlockPos blockPos = BlockPos.containing(posVec);

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        if (!OperationHelpers.checkInRange(executor, "get_block", sourcePos, posVec)) {
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, "get_block");
        if (levelOpt.isEmpty()) return;

        Level level = levelOpt.get();

        // 获取方块状态
        BlockState blockState = level.getBlockState(blockPos);

        // 创建 BlockType
        executor.pushData(BlockData.from(blockState, blockPos, level));
    }
}
