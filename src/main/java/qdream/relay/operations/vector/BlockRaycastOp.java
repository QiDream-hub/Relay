package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NullData;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 方块视线追踪操作
 * 从起点沿方向发射射线，检测击中的方块坐标
 *
 * 弹出：vector (方向), vector (起点), number (最大距离)
 * 压入：vector (击中方块坐标) 或 null (未击中)
 *
 * 需要世界交互器，并检查范围
 */
public class BlockRaycastOp extends Spell {

    public BlockRaycastOp() {
        super("relay:block_raycast", 4, 3, OperationSignature.builder()
                .consumesFromData("maxDistance", "relay:number")
                .consumesFromData("direction", "relay:vector")
                .consumesFromData("start", "relay:vector")
                .producesToData("hitPos", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, "relay:block_raycast")) {
            return;
        }

        // 弹出参数
        NumberData maxDist = OperationHelpers.popNumber(executor, "relay:block_raycast");
        if (maxDist == null) return;
        
        VectorData dir = OperationHelpers.popVector(executor, "relay:block_raycast");
        if (dir == null) return;
        
        VectorData start = OperationHelpers.popVector(executor, "relay:block_raycast");
        if (start == null) return;

        double maxDistVal = maxDist.asDouble();
        Vec3 direction = dir.asVector().normalize();
        Vec3 startPos = start.asVector();
        Vec3 endPos = startPos.add(direction.scale(maxDistVal));

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, "relay:block_raycast");
        if (levelOpt.isEmpty()) return;

        Level level = levelOpt.get();

        // 执行射线追踪
        BlockHitResult hitResult = level.clip(new ClipContext(
            startPos, endPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            CollisionContext.empty()
        ));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // 检查击中方块在范围内
            Vec3 blockCenter = Vec3.atCenterOf(hitResult.getBlockPos());
            if (!OperationHelpers.checkInRange(executor, "block_raycast", startPos, blockCenter)) {
                executor.pushData(NullData.INSTANCE);
                return;
            }
            // 返回击中的方块坐标（方块中心）
            executor.pushData(new VectorData(blockCenter));
        } else {
            executor.pushData(NullData.INSTANCE);
        }
    }
}
