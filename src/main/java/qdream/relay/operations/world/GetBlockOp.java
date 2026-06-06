package qdream.relay.operations.world;

import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.types.VectorIota;
import qdream.relay.types.NullIota;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Get Block 操作 - 获取世界中方块的类型
 * 输入：向量（坐标）
 * 输出：字符串（方块 ID）或 null
 * 需要世界交互器
 */
public class GetBlockOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!executor.hasWorldInteractor()) {
            executor.triggerMishap("操作 relay:get_block 需要世界交互器");
            return;
        }

        Executable posData = executor.popData();
        if (posData == null) return;
        if (!(posData instanceof VectorIota pos)) {
            executor.triggerMishap("操作 relay:get_block 期望 vector 类型，实际为：" + posData.getId());
            return;
        }

        Vec3 vec = pos.getVec3();
        BlockPos blockPos = BlockPos.containing(vec);

        // TODO: 获取世界中的方块
        // Level level = executor.getWorld();
        // BlockState state = level.getBlockState(blockPos);
        // String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        // executor.pushData(new StringIota(blockId));

        // 临时实现：返回 null
        executor.pushData(NullIota.INSTANCE);
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("vector")
                .output("string")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
