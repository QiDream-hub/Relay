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

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.WorldInteractorItem;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
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
        // 检查世界交互器 - 通过 shellContainer 检查
        ShellContainer container = getShellContainer(executor);
        if (container == null || !container.hasWorldInteractor()) {
            executor.triggerMishap("block_raycast 需要世界交互器");
            return;
        }

        ItemStack interactor = container.getInteractorStack();

        // 弹出参数
        Executable maxDistExe = executor.popData();
        Executable dirExe = executor.popData();
        Executable startExe = executor.popData();

        if (maxDistExe == null || dirExe == null || startExe == null) {
            executor.triggerMishap("数据栈不足，需要 number, vector, vector");
            return;
        }

        if (!(maxDistExe instanceof NumberData maxDistEx) ||
            !(dirExe instanceof VectorData dirEx) ||
            !(startExe instanceof VectorData startEx)) {
            executor.triggerMishap("期望 number, vector, vector 类型");
            return;
        }

        double maxDist = maxDistEx.asDouble();
        Vec3 direction = dirEx.asVector().normalize();
        Vec3 start = startEx.asVector();
        Vec3 end = start.add(direction.scale(maxDist));

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
            // 检查击中方块在范围内（参考 Hexcasting）
            Vec3 blockCenter = Vec3.atCenterOf(hitResult.getBlockPos());
            if (!WorldInteractorItem.isInRange(interactor, start, blockCenter)) {
                executor.pushData(NullData.INSTANCE);
                return;
            }
            // 返回击中的方块坐标（方块中心）
            executor.pushData(new VectorData(blockCenter));
        } else {
            executor.pushData(NullData.INSTANCE);
        }
    }

    /**
     * 从上下文获取 ShellContainer
     * @param executor 状态机
     * @return ShellContainer，如果不存在返回 null
     */
    private ShellContainer getShellContainer(StateMachine executor) {
        if (!executor.hasContext("shellContainer")) {
            return null;
        }
        return executor.getContext("shellContainer", ShellContainer.class).orElse(null);
    }
}
