package qdream.relay.operations.spawn;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
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
public class EntityShellAddEnergyOp extends Instruction {

    public EntityShellAddEnergyOp() {
        super("relay:entity_shell_add_energy", 1, 10, OperationSignature.builder()
                .consumesFromData("shell", "relay:entity")
                .consumesFromData("energy", "relay:number")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        if (entityData == null)
            return;

        NumberData energyNum = StackHelpers.popNumber(executor, id);
        if (energyNum == null)
            return;

        double energy = energyNum.getValue();

        // 验证能量值
        if (energy <= 0) {
            executor.triggerMishap("能量值必须大于 0: " + energy);
            return;
        }

        // 获取实体
        var entity = entityData.getEntity();
        if (entity == null) {
            executor.triggerMishap("实体引用无效");
            return;
        }

        // 检查是否为 EntityShell
        if (!(entity instanceof EntityShell shell)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 添加能量
        shell.addEnergy(energy);
        executor.pushData(new BooleanData(true));
    }
}
