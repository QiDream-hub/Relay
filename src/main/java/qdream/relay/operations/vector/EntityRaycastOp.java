package qdream.relay.operations.vector;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.WorldInteractorItem;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.EntityType;
import qdream.relay.types.NullType;
import qdream.relay.types.NumberType;
import qdream.relay.types.VectorType;

/**
 * 实体视线追踪操作
 * 从起点沿方向发射射线，检测击中的实体
 *
 * 弹出：entity (排除的实体，通常是施法者), vector (方向), vector (起点), number (最大距离)
 * 压入：entity (击中的实体) 或 null (未击中)
 *
 * 语义说明：
 * - 未击中任何实体时返回 null（这是正常行为，不是错误）
 * - 击中但超出世界交互器范围时返回 null（这是正常行为，不是错误）
 *
 * 需要世界交互器，并检查范围
 */
public class EntityRaycastOp extends Spell {

    public EntityRaycastOp() {
        super("relay:entity_raycast", 3, 5, OperationSignature.builder()
                .consumesFromData("relay:number")
                .consumesFromData("relay:vector")
                .consumesFromData("relay:vector")
                .consumesFromData("relay:entity")
                .producesToData("relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!executor.hasContext("worldInteractor")) {
            executor.triggerMishap("entity_raycast 需要世界交互器");
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
        Executable excludeExe = executor.popData();

        if (maxDistExe == null || dirExe == null || startExe == null || excludeExe == null) {
            executor.triggerMishap("数据栈不足，需要 number, vector, vector, entity");
            return;
        }

        if (!(maxDistExe instanceof NumberType maxDistEx) ||
            !(dirExe instanceof VectorType dirEx) ||
            !(startExe instanceof VectorType startEx) ||
            !(excludeExe instanceof EntityType excludeEx)) {
            executor.triggerMishap("期望 number, vector, vector, entity 类型");
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

        // 获取要排除的实体（通常是施法者自己）
        Entity excludeEntity = excludeEx.getEntity();

        // 执行射线追踪 - 使用 Minecraft 内置的实体射线检测
        Entity hitEntity = null;
        double hitDistSq = maxDist * maxDist;
        Vec3 hitPos = null;

        // 获取搜索盒内的所有实体
        AABB searchBox = new AABB(start, end).inflate(1.0);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, searchBox)) {
            // 跳过排除的实体
            if (entity == excludeEntity) {
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
            if (!WorldInteractorItem.isInRange(interactor, start, hitPos)) {
                executor.pushData(NullType.INSTANCE);
                return;
            }
            executor.pushData(EntityType.from(hitEntity, level));
        } else {
            executor.pushData(NullType.INSTANCE);
        }
    }
}
