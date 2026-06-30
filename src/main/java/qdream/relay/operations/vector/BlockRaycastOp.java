package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.WorldInteractorItem;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.NullType;
import qdream.relay.types.NumberType;
import qdream.relay.types.VectorType;

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
                .consumesFromData("relay:number")
                .consumesFromData("relay:vector")
                .consumesFromData("relay:vector")
                .producesToData("relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!executor.hasContext("worldInteractor")) {
            executor.triggerMishap("block_raycast 需要世界交互器");
            return;
        }

        Optional<ItemStack> interactorOpt = executor.getContext("worldInteractor", ItemStack.class);
        if (interactorOpt.isEmpty() || interactorOpt.get().isEmpty()) {
            executor.triggerMishap("世界交互器无效");
            return;
        }

        ItemStack interactor = interactorOpt.get();

        // 弹出参数
        Executable maxDistExe = executor.popData();
        Executable dirExe = executor.popData();
        Executable startExe = executor.popData();

        if (maxDistExe == null || dirExe == null || startExe == null) {
            executor.triggerMishap("数据栈不足，需要 number, vector, vector");
            return;
        }

        if (!(maxDistExe instanceof NumberType maxDistEx) ||
            !(dirExe instanceof VectorType dirEx) ||
            !(startExe instanceof VectorType startEx)) {
            executor.triggerMishap("期望 number, vector, vector 类型");
            return;
        }

        double maxDist = maxDistEx.asDouble();
        Vec3 direction = dirEx.asVector().normalize();
        Vec3 start = startEx.asVector();
        Vec3 end = start.add(direction.scale(maxDist));

        // 检查范围
        if (!WorldInteractorItem.isInRange(interactor, start, end)) {
            executor.pushData(NullType.INSTANCE);
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = executor.getContext("level", Level.class);
        if (levelOpt.isEmpty()) {
            executor.triggerMishap("无法获取世界");
            return;
        }

        Level level = levelOpt.get();

        // 执行射线追踪
        BlockHitResult hitResult = level.clip(new ClipContext(
            start, end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            CollisionContext.empty()
        ));

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // 返回击中的方块坐标
            executor.pushData(new VectorType(hitResult.getLocation()));
        } else {
            executor.pushData(NullType.INSTANCE);
        }
    }
}
