package qdream.relay.operations.summon.display;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.NumberData;
import qdream.relay.types.BooleanData;
import qdream.relay.entities.StringDisplay;

/**
 * 设置 StringDisplay 背景颜色操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出实体引用、颜色值和透明度</li>
 * <li>验证实体是否为 StringDisplay 类型</li>
 * <li>更新背景颜色（RGB + Alpha）</li>
 * </ul>
 *
 * <h3>颜色格式</h3>
 * <p>RGB 值范围：0-16777215 (0x000000 - 0xFFFFFF)</p>
 * <p>透明度范围：0-255 (0 = 完全透明，255 = 完全不透明)</p>
 *
 * 弹出：entity (实体引用), number (RGB 颜色值), number (透明度 0-255)
 * 压入：boolean (是否成功设置)
 */
public class SetStringDisplayBackgroundColor extends Instruction {

    public SetStringDisplayBackgroundColor() {
        super("relay:set_string_display_background_color", 1, 3, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .consumesFromData("color", "relay:number")
                .consumesFromData("alpha", "relay:number")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        NumberData colorData = StackHelpers.popNumber(executor, id);
        NumberData alphaData = StackHelpers.popNumber(executor, id);

        // 获取实体对象
        var entity = entityData.getEntity();
        if (entity == null || !(entity instanceof StringDisplay display)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 验证参数
        int rgb = (int) colorData.getValue();
        int alpha = (int) alphaData.getValue();

        if (rgb < 0 || rgb > 0xFFFFFF) {
            executor.pushData(new BooleanData(false));
            return;
        }

        if (alpha < 0 || alpha > 255) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 设置背景颜色（带透明度）
        display.setBackgroundColor(rgb, alpha);

        // 压入成功标志
        executor.pushData(new BooleanData(true));
    }
}
