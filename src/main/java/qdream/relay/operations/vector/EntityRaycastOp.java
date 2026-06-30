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
 * 需要世界交互器，并检查范围
 */
public class EntityRaycastOp extends Spell {

    public EntityRaycastOp() {
        super("relay:entity_raycast", 5, 3, OperationSignature.builder()
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

        // 获取要排除的实体（通常是施法者自己）
        Entity excludeEntity = excludeEx.getEntity();

        // 执行实体射线追踪 - 遍历射线上的所有实体
        Entity closestEntity = null;
        double closestDistance = Double.MAX_VALUE;

        // 计算搜索盒
        AABB searchBox = new AABB(start, end).inflate(1.0);
        
        // 获取搜索盒内的所有实体
        for (Entity entity : level.getEntitiesOfClass(Entity.class, searchBox)) {
            // 跳过排除的实体
            if (excludeEntity != null && entity == excludeEntity) {
                continue;
            }

            // 计算实体到射线的最近距离
            Vec3 entityPos = entity.position();
            
            // 使用简单的距离检查（可以改进为更精确的射线 - 实体相交检测）
            double dist = start.distanceTo(entityPos);
            if (dist <= maxDist && dist < closestDistance) {
                // 检查实体是否在射线方向上
                Vec3 toEntity = entityPos.subtract(start).normalize();
                double dot = toEntity.dot(direction);
                
                // 如果点积接近 1，说明实体在射线方向上
                if (dot > 0.9) {
                    closestDistance = dist;
                    closestEntity = entity;
                }
            }
        }

        if (closestEntity != null) {
            executor.pushData(EntityType.from(closestEntity, level));
        } else {
            executor.pushData(NullType.INSTANCE);
        }
    }
}
