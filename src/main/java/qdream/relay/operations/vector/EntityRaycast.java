package qdream.relay.operations.vector;

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
import qdream.relay.types.NullData;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 实体视线追踪操作
 * 从起点沿方向发射射线，检测击中的实体
 *
 * 弹出：vector (方向), vector (起点), number (最大距离)
 * 压入：entity (击中的实体) 或 null (未击中)
 *
 * 语义说明：
 * - 未击中任何实体时返回 null（这是正常行为，不是错误）
 * - 击中但超出世界交互器范围时返回 null（这是正常行为，不是错误）
 *
 * 需要世界交互器，并检查范围
 */
public class EntityRaycast extends Instruction {

    public EntityRaycast() {
        super("relay:entity_raycast", 2, 0.5, OperationSignature.builder()
                .consumesFromData("maxDistance", "relay:number")
                .consumesFromData("direction", "relay:vector")
                .consumesFromData("start", "relay:vector")
                .producesToData("hitEntity", "relay:entity")
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

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty()) {
            executor.pushData(NullData.INSTANCE);
            return;
        }

        Level level = levelOpt.get();

        // 获取要排除的实体
        Entity excludeEntity = null;
        Object self = OperationHelpers.getSelf(executor);
        if (self instanceof Entity entity) {
            excludeEntity = entity;
        }

        // 执行射线追踪 - 使用 Minecraft 内置的实体射线检测
        Entity hitEntity = null;
        double hitDistSq = maxDist * maxDist;
        Vec3 hitPos = null;

        // 获取搜索盒内的所有实体
        AABB searchBox = new AABB(start, end).inflate(1.0);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, searchBox)) {
            // 跳过排除的实体
            if (excludeEntity != null && entity == excludeEntity) {
                continue;
            }

            // 跳过施法者骑乘的载具（参考 Hexcasting）
            if (excludeEntity != null &&
                    entity.getRootVehicle() == excludeEntity.getRootVehicle()) {
                continue;
            }

            // 使用实体的碰撞箱 + pickRadius 进行射线检测（参考 Hexcasting）
            AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());

            // 计算射线与实体碰撞箱的交点
            Optional<Vec3> optionalHitPos = entityBox.clip(start, end);
            if (optionalHitPos.isEmpty()) {
                continue;
            }

            Vec3 currentHitPos = optionalHitPos.get();

            // 处理起点在碰撞箱内的情况（参考 Hexcasting）
            if (entityBox.contains(start)) {
                if (hitDistSq >= 0) {
                    hitEntity = entity;
                    hitPos = currentHitPos;
                    hitDistSq = 0;
                }
            } else {
                // 使用距离平方比较（性能优化）
                double distSq = start.distanceToSqr(currentHitPos);
                if (distSq < hitDistSq || hitDistSq == 0.0) {
                    hitEntity = entity;
                    hitPos = currentHitPos;
                    hitDistSq = distSq;
                }
            }
        }

        // 检查击中的实体是否在范围内（参考 Hexcasting）
        if (hitEntity != null && hitPos != null) {
            if (!OperationHelpers.checkInRange(executor, id, start, hitPos)) {
                executor.pushData(NullData.INSTANCE);
                return;
            }
            EntityData result = EntityData.from(hitEntity, level);
            executor.pushData(result);
        } else {
            executor.pushData(NullData.INSTANCE);
        }
    }
}
