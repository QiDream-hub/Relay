package qdream.relay.operations.spawn;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.NumberData;
import qdream.relay.entities.EntityShell;

/**
 * 停止 EntityShell 运行操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出 EntityShell 引用</li>
 * <li>验证 EntityShell 是否有效</li>
 * <li>清除 StateMachine 的程序栈，停止运行</li>
 * </ul>
 *
 * 弹出：entity (EntityShell)
 * 压入：boolean (是否成功)
 */
public class EntityShellStopOp extends Instruction {

    public EntityShellStopOp() {
        super("relay:entity_shell_stop", 1, 5, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        if (entityData == null)
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

        // 清除程序栈，停止运行
        shell.getStateMachine().clear();

        executor.pushData(new NumberData(1));
    }
}
