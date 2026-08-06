package qdream.relay.operations.summon.shell;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EntityException;
import qdream.relay.mc.errors.EnergyException;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.NumberData;
import qdream.relay.entities.EntityShell;

/**
 * 为 EntityShell 添加能量操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出 EntityShell 引用和能量值</li>
 * <li>验证 EntityShell 是否有效</li>
 * <li>为 EntityShell 添加指定能量</li>
 * </ul>
 *
 * <h3>参数约束</h3>
 * <ul>
 * <li>energy: > 0</li>
 * </ul>
 *
 * 弹出：entity (EntityShell), number (能量值)
 * 压入：boolean (是否成功)
 */
public class EntityShellAddEnergy extends Instruction {

    public EntityShellAddEnergy() {
        super("relay:entity_shell_add_energy", 1, 5, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity")
                .consumesFromData("energy", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        OperationHelpers.checkWorldInteractor(executor, id);

        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        NumberData energyNum = StackHelpers.popNumber(executor, id);

        double energy = energyNum.getValue();

        // 验证能量值
        if (energy <= 0) {
            throw new ParameterException(executor, "能量值必须大于 0: " + energy);
        }

        // 获取实体
        var entity = entityData.getEntity();
        if (entity == null) {
            throw new EntityException(executor, "实体引用无效");
        }

        // 检查是否为 EntityShell
        if (!(entity instanceof EntityShell shell)) {
            throw new EntityException(executor, "目标实体不是 EntityShell");
        }

        if (!OperationHelpers.consumeEnergy(executor, energy)) {
            throw new EnergyException(executor, "能量不足，无法为 EntityShell 添加能量");
        }

        // 添加能量
        shell.addEnergy(energy);
    }
}
