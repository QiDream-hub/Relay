package qdream.relay.operations.summon.display;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.BooleanData;
import qdream.relay.entities.StringDisplay;

/**
 * 设置 StringDisplay 追踪玩家操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出实体引用和布尔值</li>
 * <li>验证实体是否为 StringDisplay 类型</li>
 * <li>设置是否启用追踪玩家（Billboard 渲染）</li>
 * </ul>
 *
 * <h3>渲染模式</h3>
 * <ul>
 * <li>true: 使用 CENTER 模式，始终面向玩家</li>
 * <li>false: 使用 FIXED 模式，保持固定朝向</li>
 * </ul>
 *
 * 弹出：entity (实体引用), boolean (是否追踪玩家)
 * 压入：boolean (是否成功设置)
 */
public class SetStringDisplayTrackPlayer extends Instruction {

    public SetStringDisplayTrackPlayer() {
        super("relay:set_string_display_track_player", 1, 2, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .consumesFromData("track_player", "relay:boolean")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        BooleanData trackPlayerData = StackHelpers.popBoolean(executor, id);

        // 获取实体对象
        var entity = entityData.getEntity();
        if (entity == null || !(entity instanceof StringDisplay display)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 设置追踪玩家模式
        boolean trackPlayer = trackPlayerData.asBoolean();
        if (trackPlayer) {
            display.setBillboardConstraints(StringDisplay.BillboardType.CENTER);
        } else {
            display.setBillboardConstraints(StringDisplay.BillboardType.FIXED);
        }

        // 压入成功标志
        executor.pushData(new BooleanData(true));
    }
}
