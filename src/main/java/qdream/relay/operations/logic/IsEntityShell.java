package qdream.relay.operations.logic;

import net.minecraft.world.entity.Entity;

import qdream.relay.engine.StateMachine;
import qdream.relay.entities.EntityShell;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;

/**
 * IsEntityShellOp 操作 - 检测实体是否是 EntityShell
 *
 * 弹出：
 * - entity (要检测的实体)
 *
 * 压入：
 * - boolean (是否是 EntityShell)
 *
 * 示例用法：
 * 1. 检测实体类型：some_entity is_entity_shell if { ... }
 * 2. 过滤 Shell 实体：scan_entities [is_entity_shell]
 */
public class IsEntityShell extends Instruction {

    public IsEntityShell() {
        super("relay:is_entity_shell", 1, 0.05, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("isEntityShell", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        EntityData popEntity = StackHelpers.popEntity(executor, id);
        Entity entity = popEntity.getEntity();
        // 检查是否是 EntityShell
        boolean isEntityShell = (entity instanceof EntityShell);
        executor.pushData(new BooleanData(isEntityShell));
    }
}
