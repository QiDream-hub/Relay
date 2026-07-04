package qdream.relay.operations.entity;

import java.util.List;
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
import qdream.relay.types.EntityData;
import qdream.relay.types.VectorData;

/**
 * 从坐标获取实体操作
 * 检测指定位置附近的实体并返回最近的实体
 *
 * 弹出：vector (位置)
 * 压入：entity (实体引用，如果不存在则为 null)
 *
 * 需要世界交互器，并检查范围
 *
 * 示例用法：
 * 1. 获取位置实体：some_vector get_entity
 * 2. 检查是否存在实体：some_vector get_entity is_null if ...
 * 3. 获取坐标并存储：get_self get_entity_pos get_entity some_list list_append
 */
public class GetEntityOp extends Spell {

    // 检测半径（方块）
    private static final double SEARCH_RADIUS = 1.0;

    public GetEntityOp() {
        super("relay:get_entity", 1, 1, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("entity", "relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器 - 通过 shellContainer 检查
        ShellContainer container = getShellContainer(executor);
        if (container == null || !container.hasWorldInteractor()) {
            executor.triggerMishap("get_entity 需要世界交互器");
            return;
        }

        ItemStack interactor = container.getInteractorStack();

        // 弹出参数
        Executable posExe = executor.popData();

        if (posExe == null) {
            executor.triggerMishap("数据栈不足，需要 vector");
            return;
        }

        if (!(posExe instanceof VectorData posEx)) {
            executor.triggerMishap("期望 vector 类型");
            return;
        }

        Vec3 posVec = posEx.asVector();

        // 从 self 获取执行者位置作为源位置（self 可能是 Entity 或 BlockEntity）
        Vec3 sourcePos = new Vec3(0, 0, 0);
        var selfOpt = executor.getContext("self", Object.class);
        if (selfOpt.isPresent()) {
            Object self = selfOpt.get();
            if (self instanceof net.minecraft.world.entity.Entity entity) {
                sourcePos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
            } else if (self instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
                net.minecraft.core.BlockPos blockPosSelf = blockEntity.getBlockPos();
                sourcePos = new Vec3(blockPosSelf.getX() + 0.5, blockPosSelf.getY(), blockPosSelf.getZ() + 0.5);
            }
        }

        // 检查范围（检测搜索区域的边界）
        Vec3 searchEdge = posVec.add(new Vec3(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS));
        if (!WorldInteractorItem.isInRange(interactor, sourcePos, searchEdge)) {
            executor.triggerMishap("get_entity 超出世界交互器范围");
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = executor.getContext("level", Level.class);
        if (levelOpt.isEmpty()) {
            executor.triggerMishap("无法获取世界");
            return;
        }

        Level level = levelOpt.get();

        // 创建搜索区域
        AABB searchBox = new AABB(
            posVec.x - SEARCH_RADIUS, posVec.y - SEARCH_RADIUS, posVec.z - SEARCH_RADIUS,
            posVec.x + SEARCH_RADIUS, posVec.y + SEARCH_RADIUS, posVec.z + SEARCH_RADIUS
        );

        // 获取区域内的所有实体
        List<Entity> entities = level.getEntities(null, searchBox);

        if (entities.isEmpty()) {
            // 返回 null EntityType
            executor.pushData(new EntityData(null, null, null));
            return;
        }

        // 找到距离最近的实体
        Entity closestEntity = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : entities) {
            double distance = entity.position().distanceToSqr(posVec);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestEntity = entity;
            }
        }

        // 创建 EntityType
        if (closestEntity != null) {
            executor.pushData(EntityData.from(closestEntity, level));
        } else {
            executor.pushData(new EntityData(null, null, null));
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
