package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.base.OperationHelpers;
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
        super("relay:detect_block", 3, 1, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("exists", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, "relay:detect_block")) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 弹出参数
        VectorData pos = OperationHelpers.popVector(executor, "relay:detect_block");
        if (pos == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Vec3 posVec = pos.asVector();
        BlockPos blockPos = BlockPos.containing(posVec);

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        ItemStack interactor = OperationHelpers.getWorldInteractorStack(executor).orElse(ItemStack.EMPTY);
        if (!qdream.relay.items.WorldInteractorItem.isInRange(interactor, sourcePos, posVec)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, "relay:detect_block");
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
