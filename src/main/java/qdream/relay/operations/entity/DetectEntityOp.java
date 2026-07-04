package qdream.relay.operations.entity;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.base.OperationHelpers;
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
public class DetectEntityOp extends Spell {

    public DetectEntityOp() {
        super("relay:detect_entity", 3, 2, OperationSignature.builder()
                .consumesFromData("radius", "relay:number")
                .consumesFromData("center", "relay:vector")
                .producesToData("found", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 检查世界交互器
        if (!OperationHelpers.checkWorldInteractor(executor, "relay:detect_entity")) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 弹出参数
        NumberData radius = OperationHelpers.popNumber(executor, "relay:detect_entity");
        if (radius == null) {
            executor.pushData(new BooleanData(false));
            return;
        }
        
        VectorData center = OperationHelpers.popVector(executor, "relay:detect_entity");
        if (center == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        double radiusVal = radius.asDouble();
        Vec3 centerPos = center.asVector();

        // 获取源位置并检查范围
        Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
        ItemStack interactor = OperationHelpers.getWorldInteractorStack(executor).orElse(ItemStack.EMPTY);
        Vec3 searchEdge = centerPos.add(new Vec3(radiusVal, radiusVal, radiusVal));
        if (!qdream.relay.items.WorldInteractorItem.isInRange(interactor, sourcePos, searchEdge)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取 Level 上下文
        Optional<Level> levelOpt = OperationHelpers.getLevel(executor, "relay:detect_entity");
        if (levelOpt.isEmpty()) {
            executor.pushData(new BooleanData(false));
            return;
        }

        Level level = levelOpt.get();

        // 检测实体
        AABB searchBox = new AABB(
            centerPos.x - radiusVal, centerPos.y - radiusVal, centerPos.z - radiusVal,
            centerPos.x + radiusVal, centerPos.y + radiusVal, centerPos.z + radiusVal
        );

        boolean found = !level.getEntitiesOfClass(Entity.class, searchBox).isEmpty();
        executor.pushData(new BooleanData(found));
    }
}
