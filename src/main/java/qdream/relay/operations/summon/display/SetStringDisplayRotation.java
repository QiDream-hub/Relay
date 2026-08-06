package qdream.relay.operations.summon.display;

import net.minecraft.world.phys.Vec3;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.BooleanData;
import qdream.relay.types.VectorData;
import qdream.relay.entities.StringDisplay;

/**
 * 设置 StringDisplay 朝向操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出实体引用和朝向目标点</li>
 * <li>验证实体是否为 StringDisplay 类型</li>
 * <li>设置实体朝向（使用 lookAt 方法）</li>
 * <li>自动禁用追踪玩家模式（设置为 FIXED）</li>
 * </ul>
 *
 * 弹出：entity (实体引用), vector (朝向目标点)
 * 压入：boolean (是否成功设置)
 */
public class SetStringDisplayRotation extends Instruction {

    public SetStringDisplayRotation() {
        super("relay:set_string_display_rotation", 1, 2, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .consumesFromData("target", "relay:vector")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        VectorData targetData = StackHelpers.popVector(executor, id);

        // 获取实体对象
        var entity = entityData.getEntity();
        if (entity == null || !(entity instanceof StringDisplay display)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 获取目标点
        Vec3 target = targetData.asVector();
        if (target == null) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 设置朝向（覆盖 Billboard 为 FIXED）
        display.lookAt(target, true);

        // 压入成功标志
        executor.pushData(new BooleanData(true));
    }
}
