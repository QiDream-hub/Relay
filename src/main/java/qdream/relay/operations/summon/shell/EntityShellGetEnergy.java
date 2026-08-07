package qdream.relay.operations.summon.shell;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.tools.ErrorMessageTools;
import qdream.relay.tools.ErrorMessageTools.ErrorType;
import qdream.relay.types.EntityData;
import qdream.relay.types.NumberData;
import qdream.relay.entities.EntityShell;

/**
 * 获取 EntityShell 能量操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出 EntityShell 引用</li>
 * <li>验证 EntityShell 是否有效</li>
 * <li>获取 EntityShell 当前能量值</li>
 * </ul>
 *
 * 弹出：entity (EntityShell)
 * 压入：number (当前能量值，失败则为 0)
 */
public class EntityShellGetEnergy extends Instruction {

    public EntityShellGetEnergy() {
        super("relay:entity_shell_get_energy", 1, 1, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity")
                .producesToData("energy", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);

        // 获取实体
        var entity = entityData.getEntity();
        if (entity == null) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.ENTITY_REFERENCE_INVALID));
        }

        // 检查是否为 EntityShell
        if (!(entity instanceof EntityShell shell)) {
            throw new EntityException(executor, ErrorMessageTools.buildErrorMessage(ErrorType.NOT_ENTITY_SHELL));
        }

        // 获取能量
        executor.pushData(new NumberData(shell.getEnergy()));
    }
}
