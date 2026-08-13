package qdream.relay.operations.logic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;

/**
 * IsHostileOp 操作 - 检测实体是否是敌对生物（Monster）
 *
 * 弹出：
 * - entity (要检测的实体)
 *
 * 压入：
 * - boolean (是否是敌对生物)
 *
 * 示例用法：
 * 1. 检测实体类型：some_entity is_hostile if { ... }
 * 2. 过滤敌对生物：scan_entities [is_hostile]
 */
public class IsHostile extends Instruction {

    public IsHostile() {
        super("relay:is_hostile", 1, 0.05, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("isHostile", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        EntityData popEntity = StackHelpers.popEntity(executor, id);
        Entity entity = popEntity.getEntity();
        // 检查是否是敌对生物（Monster）
        boolean isHostile = (entity instanceof Monster);
        executor.pushData(new BooleanData(isHostile));
    }
}
