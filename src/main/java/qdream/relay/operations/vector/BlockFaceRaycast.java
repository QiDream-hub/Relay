package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ExecutionException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NullData;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 方块面视线追踪操作
 * 从起点沿方向发射射线，检测击中方块的那一面
 *
 * 弹出：vector (方向), vector (起点), number (最大距离)
 * 压入：string (击中的面名称：north/south/east/west/up/down) 或 null (未击中)
 *
 * 需要世界交互器，并检查范围
 */
public class BlockFaceRaycast extends Instruction {

    public BlockFaceRaycast() {
        super("relay:block_face_raycast", 2, 0.5, OperationSignature.builder()
                .consumesFromData("maxDistance", "relay:number")
                .consumesFromData("direction", "relay:vector")
                .consumesFromData("start", "relay:vector")
                .producesToData("hitFace", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        OperationHelpers.checkWorldInteractor(executor, id);

        // 弹出参数
        NumberData maxDist = StackHelpers.popNumber(executor, id);
        VectorData dir = StackHelpers.popVector(executor, id);
        VectorData start = StackHelpers.popVector(executor, id);

        double maxDistVal = maxDist.asDouble();
        Vec3 direction = dir.asVector().normalize();
        Vec3 startPos = start.asVector();
        Vec3 endPos = startPos.add(direction.scale(maxDistVal));

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty())
            return;

        Level level = levelOpt.get();

        // 执行射线追踪
        BlockHitResult hitResult = level.clip(new ClipContext(
                startPos, endPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // 检查击中方块在范围内
            Vec3 blockCenter = Vec3.atCenterOf(hitResult.getBlockPos());
            try { OperationHelpers.checkInRange(executor, id, startPos, blockCenter); } catch (Exception e) { 
                executor.pushData(NullData.INSTANCE);
                return; }

            // 获取击中的面
            Direction hitFace = hitResult.getDirection();
            if (hitFace != null) {
                // 返回面的名称（小写）
                // getDirection() 返回的是击中面朝向射线来源的方向，直接使用
                executor.pushData(new VectorData(hitFace.getUnitVec3()));
            }
        } else {
            throw new ExecutionException(executor,"无法获取面");
        }
    }
}
