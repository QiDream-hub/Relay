package qdream.relay.operations.summon.shell;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.ListData;
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
 * </ul>
 *
 * 弹出：entity (EntityShell), list (新程序)
 */
public class EntityShellReset extends Instruction {

    public EntityShellReset() {
        super("relay:entity_shell_reset", 1, 2, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity")
                .consumesFromData("program", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        ListData programList = StackHelpers.popList(executor, id);

        // 获取实体
        var entity = entityData.getEntity();
        if (entity == null) {
            throw new EntityException(executor, "实体引用无效");
        }

        // 检查是否为 EntityShell
        if (!(entity instanceof EntityShell shell)) {
            throw new EntityException(executor, "目标实体不是 EntityShell");
        }

        // 清除当前程序并加载新程序
        StateMachine machine = shell.getStateMachine();
        machine.clear();
        machine.loadProgram(programList.getValue());
    }
}
