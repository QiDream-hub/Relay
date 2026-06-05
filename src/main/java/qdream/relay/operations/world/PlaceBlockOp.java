package qdream.relay.operations.world;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.McVec3Adapter;

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
        Iota blockIdIota = executor.popData();
        Iota posIota = executor.popData();

        if (blockIdIota == null || posIota == null) {
            throw new IllegalArgumentException("place_block 需要坐标和方块 ID 参数");
        }

        if (!posIota.isVector()) {
            throw new IllegalArgumentException("place_block 第一个参数需要是向量");
        }

        if (!blockIdIota.isString()) {
            throw new IllegalArgumentException("place_block 第二个参数需要是字符串");
        }

        Vec3 pos = ((McVec3Adapter) posIota.asVector()).getVec3();
        BlockPos blockPos = BlockPos.containing(pos);
        String blockId = blockIdIota.asString();

        // TODO: 实现方块放置逻辑
        // Level level = executor.getWorld();
        // Block block = BuiltInRegistries.BLOCK.get(Identifier.tryBySeparator(':', blockId).orElseThrow());
        // boolean success = level.setBlock(blockPos, block.defaultBlockState(), 3);
        // executor.pushData(Iota.ofBoolean(success));

        // 临时实现：返回 false
        executor.pushData(Iota.ofBoolean(false));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.VECTOR)
                .input(IotaType.STRING)
                .output(IotaType.BOOLEAN)
                .build();
    }

    @Override
    public int getCost() {
        return 5;
    }
}
