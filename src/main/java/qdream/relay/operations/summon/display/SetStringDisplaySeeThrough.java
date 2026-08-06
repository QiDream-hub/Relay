package qdream.relay.operations.summon.display;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.BooleanData;
import qdream.relay.entities.StringDisplay;

/**
 * 设置 StringDisplay 透视模式操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出实体引用和布尔值</li>
 * <li>验证实体是否为 StringDisplay 类型</li>
 * <li>设置是否启用透视渲染（穿墙可见）</li>
 * </ul>
 *
 * 弹出：entity (实体引用), boolean (是否透视)
 * 压入：boolean (是否成功设置)
 */
public class SetStringDisplaySeeThrough extends Instruction {

    public SetStringDisplaySeeThrough() {
        super("relay:set_string_display_see_through", 1, 2, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .consumesFromData("see_through", "relay:boolean")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        BooleanData seeThroughData = StackHelpers.popBoolean(executor, id);

        // 获取实体对象
        var entity = entityData.getEntity();
        if (entity == null || !(entity instanceof StringDisplay display)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 设置透视模式
        display.setSeeThrough(seeThroughData.asBoolean());

        // 压入成功标志
        executor.pushData(new BooleanData(true));
    }
}
