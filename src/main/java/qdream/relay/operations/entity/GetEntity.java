package qdream.relay.operations.entity;

import java.util.List;
import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.operations.OperationHelpers;
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
 */
public class GetEntity extends Instruction {

    // 检测半径（方块）
    private static final double SEARCH_RADIUS = 1.0;

    public GetEntity() {
        super("relay:get_entity", 1, 1, OperationSignature.builder()
                .consumesFromData("position", "relay:vector")
                .producesToData("entity", "relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        OperationHelpers.checkWorldInteractor(executor, id);

        // 弹出参数
        VectorData pos = StackHelpers.popVector(executor, id);

        Vec3 posVec = pos.asVector();

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        Vec3 searchEdge = posVec.add(new Vec3(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS));
        OperationHelpers.checkInRange(executor, id, sourcePos, searchEdge);

        // 获取 Level 上下文
        Level level = OperationHelpers.getLevel(executor, id);

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
}
