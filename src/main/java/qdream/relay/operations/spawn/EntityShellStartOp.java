package qdream.relay.operations.spawn;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;
import qdream.relay.entities.EntityShell;

/**
 * 启动 EntityShell 运行操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出 EntityShell 引用和程序列表</li>
 * <li>验证 EntityShell 是否有效</li>
 * <li>加载程序到 StateMachine</li>
 * <li>设置 initialized 状态为 true</li>
 * </ul>
 *
 * <h3>与 Reset 的区别</h3>
 * <p>
 * Reset 会先清除当前程序再加载新程序，而 Start 只在未运行时加载程序。
 * 如果 EntityShell 已经在运行，Start 操作不会改变其程序。
 * </p>
 *
 * 弹出：entity (EntityShell), list (程序)
 * 压入：boolean (是否成功)
 */
public class EntityShellStartOp extends Instruction {

    public EntityShellStartOp() {
        super("relay:entity_shell_start", 1, 5, OperationSignature.builder()
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
            executor.pushData(new NumberData(0));
            return;
        }

        // 检查是否为 EntityShell
        if (!(entity instanceof EntityShell shell)) {
            executor.triggerMishap("目标实体不是 EntityShell");
            executor.pushData(new NumberData(0));
            return;
        }

        // 如果已经在运行，拒绝启动
        if (shell.isRunning()) {
            executor.triggerMishap("EntityShell 已在运行中");
            executor.pushData(new NumberData(0));
            return;
        }

        // 加载程序
        StateMachine machine = shell.getStateMachine();
        machine.loadProgram(programList.getValue());

        executor.pushData(new NumberData(1));
    }
}
