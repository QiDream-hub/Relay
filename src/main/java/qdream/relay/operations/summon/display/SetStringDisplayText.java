package qdream.relay.operations.summon.display;

import net.minecraft.world.level.Level;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.errors.WorldInteractionException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.entities.StringDisplay;
import qdream.relay.types.EntityData;
import qdream.relay.types.StringData;

/**
 * 设置 StringDisplay 文本操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出实体引用和文本</li>
 * <li>验证实体是否为 StringDisplay 类型</li>
 * <li>更新显示的文本</li>
 * </ul>
 *
 * 弹出：entity (实体引用), string (新文本)
 * 压入：boolean (是否成功设置)
 */
public class SetStringDisplayText extends Instruction {

    public SetStringDisplayText() {
        super("relay:set_string_display_text", 1, 2, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .consumesFromData("text", "relay:string")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        StringData textData = StackHelpers.popString(executor, id);

        // 获取实体对象
        var entity = entityData.getEntity();
        if (entity == null || !(entity instanceof StringDisplay display)) {
            executor.pushData(new qdream.relay.types.BooleanData(false));
            return;
        }

        // 验证文本
        String text = textData.getValue();
        if (text == null || text.isEmpty()) {
            executor.pushData(new qdream.relay.types.BooleanData(false));
            return;
        }

        // 设置文本
        display.setTextString(text);

        // 压入成功标志
        executor.pushData(new qdream.relay.types.BooleanData(true));
    }
}
