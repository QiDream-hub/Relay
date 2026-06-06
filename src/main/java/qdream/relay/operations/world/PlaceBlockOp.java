package qdream.relay.operations.world;

import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.types.VectorIota;
import qdream.relay.types.StringIota;
import qdream.relay.types.BooleanIota;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Place Block 操作 - 在指定位置放置方块
 * 输入：向量（坐标），字符串（方块 ID）
 * 输出：布尔（是否成功）
 * 需要世界交互器
 */
public class PlaceBlockOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable blockIdData = executor.popData();
        if (blockIdData == null) return;
        if (!(blockIdData instanceof StringIota blockIdIota)) {
            executor.triggerMishap("操作 relay:place_block 期望 string 类型，实际为：" + blockIdData.getType());
            return;
        }
        Executable posData = executor.popData();
        if (posData == null) return;
        if (!(posData instanceof VectorIota pos)) {
            executor.triggerMishap("操作 relay:place_block 期望 vector 类型，实际为：" + posData.getType());
            return;
        }

        Vec3 vec = pos.getVec3();
        BlockPos blockPos = BlockPos.containing(vec);
        String blockIdStr = blockIdIota.asString();

        // TODO: 实现方块放置逻辑
        // Level level = executor.getWorld();
        // Block block = BuiltInRegistries.BLOCK.get(Identifier.tryBySeparator(':', blockIdStr).orElseThrow());
        // boolean success = level.setBlock(blockPos, block.defaultBlockState(), 3);
        // executor.pushData(new BooleanIota(success));

        // 临时实现：返回 false
        executor.pushData(new BooleanIota(false));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("vector")
                .input("string")
                .output("boolean")
                .build();
    }

    @Override
    public int getCost() {
        return 5;
    }
}
