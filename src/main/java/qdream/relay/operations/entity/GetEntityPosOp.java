package qdream.relay.operations.entity;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.EntityType;
import qdream.relay.types.VectorType;

/**
 * 获取实体坐标操作
 * 获取实体当前的位置坐标
 *
 * 弹出：entity
 * 压入：vector (实体位置坐标)
 *
 * 示例用法：
 * 1. 获取自身坐标：get_self get_entity_pos
 * 2. 获取实体坐标并存储：get_self get_entity_pos some_list list_append
 * 3. 检查坐标高度：get_self get_entity_pos get_y 64 gt
 */
public class GetEntityPosOp extends Spell {

    public GetEntityPosOp() {
        super("relay:get_entity_pos", 1, 1, OperationSignature.builder()
                .consumesFromData("relay:entity")
                .producesToData("relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable entityExe = executor.popData();

        if (entityExe == null) {
            executor.triggerMishap("数据栈不足，需要 entity");
            return;
        }

        if (!(entityExe instanceof EntityType entityEx)) {
            executor.triggerMishap("期望 entity 类型");
            return;
        }

        var entity = entityEx.getEntity();
        if (entity == null) {
            executor.triggerMishap("实体引用无效");
            return;
        }

        // 获取实体位置
        var pos = entity.position();
        executor.pushData(new VectorType(pos));
    }
}
