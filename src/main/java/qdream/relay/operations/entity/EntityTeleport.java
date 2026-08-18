package qdream.relay.operations.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import qdream.relay.Relay;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BlockData;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;
import qdream.relay.types.VectorData;

/**
 * 实体传送操作
 * 将指定实体传送到目标位置
 *
 * 弹出：entity (目标实体), destination (目标位置 - BlockData 或 VectorData)
 * 压入：boolean (是否成功传送)
 *
 * 需要世界交互器，并检查目标位置到施法者的距离
 *
 * 能量消耗：
 * - 基础消耗：1
 * - 距离消耗：距离 × 0.1
 * - 跨维度消耗：额外 100（仅当使用 BlockData 且维度不同时）
 *
 * 行为说明：
 * - 接受 BlockData：传送到方块位置的中心，支持跨维度传送
 * - 接受 VectorData：传送到绝对坐标，仅在当前维度传送
 */
public class EntityTeleport extends Instruction {

    // 跨维度传送的额外能量消耗
    private static final double INTER_DIMENSION_COST = 100.0;

    public EntityTeleport() {
        super("relay:entity_teleport", 1, 2, OperationSignature.builder()
                .consumesFromData("targetEntity", "relay:entity")
                .consumesFromData("destination", "relay:block", "relay:vector")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        try {
            OperationHelpers.checkWorldInteractor(executor, id);
        } catch (Exception e) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取施法者位置和所属者
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        Entity owner = OperationHelpers.getOwner(executor);
        if (owner == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        ServerLevel sourceLevel = (ServerLevel) owner.level();
        if (sourceLevel == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 弹出参数
        Executable entityExe = StackHelpers.popAny(executor, id);
        Executable destExe = StackHelpers.popAny(executor, id);

        // 验证实体参数
        if (!(entityExe instanceof EntityData entityData)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Entity targetEntity = entityData.getEntity();
        if (targetEntity == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 处理目标位置
        Vec3 targetPos;
        ServerLevel targetLevel = sourceLevel;
        boolean isInterDimensional = false;

        if (destExe instanceof BlockData blockData) {
            // BlockData：使用方块位置，支持跨维度
            BlockPos blockPos = blockData.getBlockPos();
            String worldId = blockData.getWorldId();

            if (blockPos == null || worldId == null) {
                executor.pushData(new BooleanData(false));
                return;
            }

            // 解析目标维度
            targetLevel = Relay.getWorld(worldId);
            if (targetLevel == null) {
                executor.pushData(new BooleanData(false));
                return;
            }

            targetPos = Vec3.atCenterOf(blockPos);
            isInterDimensional = !sourceLevel.dimension().equals(targetLevel.dimension());
        } else if (destExe instanceof VectorData vectorData) {
            // VectorData：使用绝对坐标，仅当前维度
            targetPos = vectorData.asVector();
            if (targetPos == null) {
                executor.pushData(new BooleanData(false));
                return;
            }
        } else {
            // 无效的目标类型
            executor.pushData(new BooleanData(false));
            return;
        }

        // 检查范围：施法者到目标位置的距离
        try {
            OperationHelpers.checkInRange(executor, id, sourcePos, targetPos);
        } catch (Exception e) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 计算能量消耗
        double distance = sourcePos.distanceTo(targetPos);
        double distanceCost = distance * 0.1;
        double totalCost = distanceCost + (isInterDimensional ? INTER_DIMENSION_COST : 0);

        try {
            OperationHelpers.checkEnergy(executor, id, totalCost);
        } catch (Exception e) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 执行传送
        boolean success = teleportEntity(targetEntity, targetLevel, targetPos);
        executor.pushData(new BooleanData(success));
    }

    /**
     * 执行实体传送
     *
     * @param entity      要传送的实体
     * @param targetLevel 目标维度
     * @param targetPos   目标位置
     * @return 传送成功返回 true
     */
    private boolean teleportEntity(Entity entity, ServerLevel targetLevel, Vec3 targetPos) {
        try {
            if (entity.level() == targetLevel) {
                // 同维度传送：直接设置位置
                entity.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            } else {
                // 跨维度传送：使用 Minecraft 的传送系统
                // 这会正确处理实体的维度切换、乘客、ridden 关系等
                entity.teleport(new net.minecraft.world.level.portal.TeleportTransition(
                        targetLevel,
                        targetPos,
                        entity.getDeltaMovement(),
                        entity.getYRot(),
                        entity.getXRot(),
                        java.util.Set.of(),
                        net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            }
            return true;
        } catch (Exception e) {
            Relay.LOGGER.error("传送失败：{}", e.getMessage());
            return false;
        }
    }
}
