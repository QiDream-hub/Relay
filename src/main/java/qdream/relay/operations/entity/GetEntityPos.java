package qdream.relay.operations.entity;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.VectorData;

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
public class GetEntityPos extends Instruction {

    public GetEntityPos() {
        super("relay:get_entity_pos", 1, 1, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .producesToData("position", "relay:vector")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        EntityData popEntity = StackHelpers.popEntity(executor, id);

        var entity = popEntity.getEntity();
        if (entity == null) {
            throw new EntityException(id + " 错误的实体");
        }

        // 获取实体位置
        var pos = entity.position();
        executor.pushData(new VectorData(pos));
    }
}
