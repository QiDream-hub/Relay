package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.WorldInteractorItem;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BooleanType;
import qdream.relay.types.NumberType;
import qdream.relay.types.VectorType;

/**
 * 实体检测操作
 * 检测指定位置附近是否存在实体
 *
 * 弹出：vector (中心位置), number (搜索半径)
 * 压入：boolean (是否存在实体)
 *
 * 需要世界交互器，并检查范围
 */
public class DetectEntityOp extends Spell {

    public DetectEntityOp() {
        super("relay:detect_entity", 3, 2, OperationSignature.builder()
                .consumesFromData("relay:number")
                .consumesFromData("relay:vector")
                .producesToData("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器 - 通过 shellContainer 检查
        ShellContainer container = getShellContainer(executor);
        if (container == null || !container.hasWorldInteractor()) {
            executor.triggerMishap("detect_entity 需要世界交互器");
            return;
        }

        ItemStack interactor = container.getInteractorStack();
        
        // 弹出参数
        Executable radiusExe = executor.popData();
        Executable centerExe = executor.popData();
        
        if (radiusExe == null || centerExe == null) {
            executor.triggerMishap("数据栈不足，需要 number, vector");
            return;
        }
        
        if (!(radiusExe instanceof NumberType radiusEx) || 
            !(centerExe instanceof VectorType centerEx)) {
            executor.triggerMishap("期望 number, vector 类型");
            return;
        }
        
        double radius = radiusEx.asDouble();
        Vec3 center = centerEx.asVector();

        // 从 self 获取执行者位置作为源位置（self 可能是 Entity 或 BlockEntity）
        Vec3 sourcePos = new Vec3(0, 0, 0);
        var selfOpt = executor.getContext("self", Object.class);
        if (selfOpt.isPresent()) {
            Object self = selfOpt.get();
            if (self instanceof net.minecraft.world.entity.Entity entity) {
                sourcePos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            } else if (self instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
                net.minecraft.core.BlockPos blockPos = blockEntity.getBlockPos();
                sourcePos = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
            }
        }

        // 检查范围（检测搜索区域的边界）
        Vec3 searchEdge = center.add(new Vec3(radius, radius, radius));
        if (!WorldInteractorItem.isInRange(interactor, sourcePos, searchEdge)) {
            executor.pushData(new BooleanType(false));
            return;
        }
        
        // 获取 Level 上下文
        Optional<Level> levelOpt = executor.getContext("level", Level.class);
        if (levelOpt.isEmpty()) {
            executor.triggerMishap("无法获取世界");
            return;
        }
        
        Level level = levelOpt.get();
        
        // 检测实体
        AABB searchBox = new AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        );

        boolean found = !level.getEntitiesOfClass(Entity.class, searchBox).isEmpty();
        executor.pushData(new BooleanType(found));
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
