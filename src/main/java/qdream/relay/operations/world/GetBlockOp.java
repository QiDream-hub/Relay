package qdream.relay.operations.world;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.McVec3Adapter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
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
        Iota posIota = executor.popData();

        if (posIota == null) {
            throw new IllegalArgumentException("get_block 需要向量参数");
        }

        if (!posIota.isVector()) {
            throw new IllegalArgumentException("get_block 需要向量参数，得到：" + posIota.getType());
        }

        Vec3 pos = ((McVec3Adapter) posIota.asVector()).getVec3();
        BlockPos blockPos = BlockPos.containing(pos);

        // TODO: 获取世界中的方块
        // Level level = executor.getWorld();
        // BlockState state = level.getBlockState(blockPos);
        // String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        // executor.pushData(Iota.ofString(blockId));

        // 临时实现：返回 null
        executor.pushData(Iota.ofNull());
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.VECTOR)
                .output(IotaType.STRING)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
