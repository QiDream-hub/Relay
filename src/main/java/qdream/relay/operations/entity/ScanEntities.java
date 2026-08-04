package qdream.relay.operations.entity;

import java.util.List;
import java.util.ArrayList;

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
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;
import qdream.relay.types.VectorData;

/**
 * 范围实体扫描操作
 * 扫描指定范围内的所有实体并返回实体列表
 *
 * 弹出：center (中心位置), radius (扫描半径)
 * 压入：entities (实体列表)
 *
 * 需要世界交互器，并检查范围
 */
public class ScanEntities extends Instruction {

    public ScanEntities() {
        super("relay:scan_entities", 2, 1, OperationSignature.builder()
                .consumesFromData("radius", "relay:number")
                .consumesFromData("center", "relay:vector")
                .producesToData("entities", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        try { OperationHelpers.checkWorldInteractor(executor, id); } catch (Exception e) {
            executor.pushData(new ListData(new ArrayList<>()));
            return; }

        VectorData centerData = StackHelpers.popVector(executor, id);
        NumberData radiusData = StackHelpers.popNumber(executor, id);

        double radius = radiusData.asDouble();
        Vec3 center = centerData.asVector();

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        Vec3 searchEdge = center.add(new Vec3(radius, radius, radius));
        try { OperationHelpers.checkInRange(executor, id, sourcePos, searchEdge); } catch (Exception e) { 
            executor.pushData(new ListData(new ArrayList<>()));
            return; }

        // 获取 Level 上下文
        var levelOpt = OperationHelpers.getLevel(executor, id);
        if (levelOpt.isEmpty()) {
            executor.pushData(new ListData(new ArrayList<>()));
            return;
        }

        Level level = levelOpt.get();

        // 创建搜索区域
        AABB searchBox = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);

        // 获取区域内的所有实体
        List<Entity> entities = level.getEntities(null, searchBox);

        // 创建实体列表
        List<qdream.relay.engine.Executable> resultList = new ArrayList<>();
        for (Entity entity : entities) {
            resultList.add(EntityData.from(entity, level));
        }

        executor.pushData(new ListData(resultList));
    }
}
