package qdream.relay.operations.logic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;

/**
 * IsAnimalOp 操作 - 检测实体是否是动物
 *
 * 弹出：
 * - entity (要检测的实体)
 *
 * 压入：
 * - boolean (是否是动物)
 *
 * 示例用法：
 * 1. 检测实体类型：some_entity is_animal if { ... }
 * 2. 过滤动物实体：scan_entities [is_animal]
 */
public class IsAnimal extends Instruction {

    public IsAnimal() {
        super("relay:is_animal", 1, 0.05, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("isAnimal", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        EntityData popEntity = StackHelpers.popEntity(executor, id);
        Entity entity = popEntity.getEntity();
        // 检查是否是动物
        boolean isAnimal = (entity instanceof Animal);
        executor.pushData(new BooleanData(isAnimal));
    }
}
