package qdream.relay.operations.world;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;

import com.google.gson.JsonObject;

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
public class PlaceBlockOp implements Executable {
    private static final String ID = "relay:place_block";

    private static final int COST = 5;

    private static final OperationSignature SIGNATURE = OperationSignature.builder()
            .input("vector")
            .input("string")
            .output("boolean")
            .build();

    public String getId() {
        return ID;
    }

    public int getCost() {
        return COST;
    }

    public OperationSignature getSignature() {
        return SIGNATURE;
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!executor.hasWorldInteractor()) {
            executor.triggerMishap("操作 relay:place_block 需要世界交互器");
            return;
        }

        Executable blockIdData = executor.popData();
        if (blockIdData == null) return;
        if (!(blockIdData instanceof StringIota blockIdIota)) {
            executor.triggerMishap("操作 relay:place_block 期望 string 类型，实际为：" + blockIdData.getId());
            return;
        }
        Executable posData = executor.popData();
        if (posData == null) return;
        if (!(posData instanceof VectorIota pos)) {
            executor.triggerMishap("操作 relay:place_block 期望 vector 类型，实际为：" + posData.getId());
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
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", getId());
        return json;
    }

    @Override
    public Executable fromJson(JsonObject json) {
        String id = json.get("id").getAsString();
        if (!ID.equals(id)) {
            throw new IllegalArgumentException("Invalid ID for PlaceBlockOp: " + id);
        }
        return new PlaceBlockOp();
    }
}
