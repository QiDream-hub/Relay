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
        try {
            OperationHelpers.checkWorldInteractor(executor, id);
        } catch (Exception e) {
            executor.pushData(new ListData(new ArrayList<>()));
            return;
        }

        // 弹出参数 (注意：栈是后进先出，所以先弹出 radius，后弹出 center)
        NumberData radiusData = StackHelpers.popNumber(executor, id);
        VectorData centerData = StackHelpers.popVector(executor, id);

        double radius = radiusData.asDouble();
        Vec3 center = centerData.asVector();

        // 获取源位置并检查范围 (检查中心点而非边缘)
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        try {
            OperationHelpers.checkInRange(executor, id, sourcePos, center);
        } catch (Exception e) {
            executor.pushData(new ListData(new ArrayList<>()));
            return;
        }

        // 获取 Level 上下文
        Level level = OperationHelpers.getLevel(executor, id);

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
