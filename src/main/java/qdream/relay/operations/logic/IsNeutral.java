package qdream.relay.operations.logic;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;

/**
 * IsNeutralOp 操作 - 检测实体是否是中立生物
 *
 * 弹出：
 * - entity (要检测的实体)
 *
 * 压入：
 * - boolean (是否是中立生物)
 *
 * 示例用法：
 * 1. 检测实体类型：some_entity is_neutral if { ... }
 * 2. 过滤中立生物：scan_entities [is_neutral]
 */
public class IsNeutral extends Instruction {

    public IsNeutral() {
        super("relay:is_neutral", 1, 0.05, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("isNeutral", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        EntityData popEntity = StackHelpers.popEntity(executor, id);
        Entity entity = popEntity.getEntity();
        // 检查是否是中立生物（NeutralMob）
        boolean isNeutral = (entity instanceof NeutralMob);
        executor.pushData(new BooleanData(isNeutral));
    }
}
