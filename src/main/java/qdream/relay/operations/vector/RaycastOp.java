package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NullData;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 视线追踪操作（Raycast）
 * 从起点沿方向发射射线，检测是否击中方块
 *
 * 弹出：vector (方向), vector (起点), number (最大距离)
 * 压入：vector (击中点) 或 null (未击中)
 *
 * 需要世界交互器，并检查范围
 */
public class RaycastOp extends Instruction {

    public RaycastOp() {
        super("relay:raycast", 2, 0.25, OperationSignature.builder()
                .consumesFromData("maxDistance", "relay:number")
                .consumesFromData("direction", "relay:vector")
                .consumesFromData("start", "relay:vector")
                .producesToData("hitPos", "relay:vector", "relay:null")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, id)) {
            return;
        }

        // 弹出参数
        NumberData maxDistData = StackHelpers.popNumber(executor, id);
        if (maxDistData == null)
            return;

        VectorData dir = StackHelpers.popVector(executor, id);
        if (dir == null)
            return;

        VectorData startData = StackHelpers.popVector(executor, id);
        if (startData == null)
            return;

        double maxDist = maxDistData.asDouble();
        Vec3 direction = dir.asVector().normalize();
        Vec3 start = startData.asVector();
        Vec3 end = start.add(direction.scale(maxDist));

        // 检查范围
        if (!OperationHelpers.checkInRange(executor, id, start, end)) {
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty())
            return;

        Level level = levelOpt.get();

        // 执行射线追踪
        BlockHitResult hitResult = level.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            executor.pushData(new VectorData(hitResult.getLocation()));
        } else {
            executor.pushData(NullData.INSTANCE);
        }
    }
}
