package qdream.relay.operations.spawn;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.EntityData;
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;
import qdream.relay.entities.EntityShell;

/**
 * 复位 EntityShell 操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出 EntityShell 引用和新程序列表</li>
 * <li>验证 EntityShell 是否有效</li>
 * <li>清除 StateMachine 的当前程序</li>
 * <li>加载新程序到 StateMachine</li>
 * <li>设置 initialized 状态为 true</li>
 * </ul>
 *
 * 弹出：entity (EntityShell), list (新程序)
 * 压入：boolean (是否成功)
 */
public class EntityShellResetOp extends Instruction {

    public EntityShellResetOp() {
        super("relay:entity_shell_reset", 1, 10, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity")
                .consumesFromData("program", "relay:list")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        if (entityData == null)
            return;

        ListData programList = StackHelpers.popList(executor, id);
        if (programList == null)
            return;

        // 获取实体
        var entity = entityData.getEntity();
        if (entity == null) {
            executor.triggerMishap("实体引用无效");
            executor.pushData(new BooleanData(false));
            return;
        }

        // 检查是否为 EntityShell
        if (!(entity instanceof EntityShell shell)) {
            executor.triggerMishap("目标实体不是 EntityShell");
            return;
        }

        // 清除当前程序并加载新程序
        StateMachine machine = shell.getStateMachine();
        machine.clear();
        machine.loadProgram(programList.getValue());

        executor.pushData(new BooleanData(true));
    }
}
