package qdream.relay.operations.logic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;

/**
 * IsItemOp 操作 - 检测实体是否是物品（掉落物）
 *
 * 弹出：
 * - entity (要检测的实体)
 *
 * 压入：
 * - boolean (是否是物品实体)
 *
 * 示例用法：
 * 1. 检测实体类型：some_entity is_item if { ... }
 * 2. 过滤物品实体：scan_entities [is_item]
 */
public class IsItem extends Instruction {

    public IsItem() {
        super("relay:is_item", 1, 0.05, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("isItem", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        EntityData popEntity = StackHelpers.popEntity(executor, id);
        Entity entity = popEntity.getEntity();
        // 检查是否是物品实体（掉落物）
        boolean isItem = (entity instanceof ItemEntity);
        executor.pushData(new BooleanData(isItem));
    }
}
