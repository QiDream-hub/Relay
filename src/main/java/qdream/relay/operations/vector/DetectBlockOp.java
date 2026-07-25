package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.VectorData;

/**
 * 方块检测操作
 * 检测指定位置是否存在方块
 *
 * 弹出：vector (位置)
 * 压入：boolean (是否存在方块)
 *
 * 需要世界交互器，并检查范围
 */
public class DetectBlockOp extends Spell {

    public DetectBlockOp() {
        super("relay:detect_block", 1, 0.25, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("exists", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 弹出参数
        VectorData pos = StackHelpers.popVector(executor, id);
        if (pos == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Vec3 posVec = pos.asVector();
        BlockPos blockPos = BlockPos.containing(posVec);

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        if (!OperationHelpers.checkInRange(executor, id, sourcePos, posVec)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Level level = levelOpt.get();

        // 检测方块
        BlockState state = level.getBlockState(blockPos);
        boolean exists = !state.isAir();

        executor.pushData(new BooleanData(exists));
    }
}
