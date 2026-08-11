package qdream.relay.operations.entity;

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
import qdream.relay.types.BooleanData;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 实体检测操作
 * 检测指定位置附近是否存在实体
 *
 * 弹出：vector (中心位置), number (搜索半径)
 * 压入：boolean (是否存在实体)
 *
 * 需要世界交互器，并检查范围
 */
public class DetectEntity extends Instruction {

    public DetectEntity() {
        super("relay:detect_entity", 2, 1, OperationSignature.builder()
                .consumesFromData("radius", "relay:number")
                .consumesFromData("center", "relay:vector")
                .producesToData("found", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        try { OperationHelpers.checkWorldInteractor(executor, id); } catch (Exception e) {
            executor.pushData(new BooleanData(false));
            return; }

        // 弹出参数
        NumberData radius = StackHelpers.popNumber(executor, id);
        VectorData center = StackHelpers.popVector(executor, id);

        double radiusVal = radius.asDouble();
        Vec3 centerPos = center.asVector();

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        Vec3 searchEdge = centerPos.add(new Vec3(radiusVal, radiusVal, radiusVal));
        OperationHelpers.checkInRange(executor, id, sourcePos, searchEdge);

        // 获取 Level 上下文
        Level level = OperationHelpers.getLevel(executor, id);

        // 检测实体
        AABB searchBox = new AABB(
                centerPos.x - radiusVal, centerPos.y - radiusVal, centerPos.z - radiusVal,
                centerPos.x + radiusVal, centerPos.y + radiusVal, centerPos.z + radiusVal);

        boolean found = !level.getEntitiesOfClass(Entity.class, searchBox).isEmpty();
        executor.pushData(new BooleanData(found));
    }
}
