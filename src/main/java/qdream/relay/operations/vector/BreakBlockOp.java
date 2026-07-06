package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.VectorData;

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
        super("relay:break_block", 1, 10, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, "break_block")) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 弹出参数
        VectorData posData = OperationHelpers.popVector(executor, "break_block");
        if (posData == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Vec3 posVec = posData.asVector();
        // 使用 containing 正确处理负数坐标（向下取整而非向零取整）
        BlockPos pos = BlockPos.containing(posVec);

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        if (!OperationHelpers.checkInRange(executor, "break_block", sourcePos, posVec)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, "break_block");
        if (levelOpt.isEmpty()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Level level = levelOpt.get();

        // 挖掘方块
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 破坏方块（基础版本，无附魔）
        // destroyBlock 参数：位置，是否掉落物品，破坏者实体，更新限制
        boolean destroyed = level.destroyBlock(pos, true, null, 512);
        executor.pushData(new BooleanData(destroyed));
    }
}
