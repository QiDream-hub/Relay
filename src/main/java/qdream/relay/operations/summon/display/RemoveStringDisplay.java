package qdream.relay.operations.summon.display;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;

/**
 * 移除 StringDisplay 实体操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出实体引用</li>
 * <li>验证实体是否为 StringDisplay 类型</li>
 * <li>调用 discard() 移除实体</li>
 * <li>不返还能量（与 SpawnStringDisplay 配对）</li>
 * </ul>
 *
 * 弹出：entity (StringDisplay 实体引用)
 * 压入：boolean (是否成功移除)
 */
public class RemoveStringDisplay extends Instruction {

    public RemoveStringDisplay() {
        super("relay:remove_string_display", 1, 5, OperationSignature.builder()
                .consumesFromData("display", "relay:entity")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出实体引用
        EntityData entityData = StackHelpers.popEntity(executor, id);

        // 获取实体对象
        var entity = entityData.getEntity();
        if (entity == null) {
            executor.pushData(new qdream.relay.types.BooleanData(false));
            return;
        }

        // 检查是否为 StringDisplay 类型
        if (!(entity instanceof qdream.relay.entities.StringDisplay)) {
            executor.pushData(new qdream.relay.types.BooleanData(false));
            return;
        }

        // 移除实体
        entity.discard();

        // 压入成功标志
        executor.pushData(new qdream.relay.types.BooleanData(true));
    }
}
