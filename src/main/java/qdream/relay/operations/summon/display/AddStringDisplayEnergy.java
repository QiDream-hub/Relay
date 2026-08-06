package qdream.relay.operations.summon.display;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.errors.EnergyException;
import qdream.relay.mc.errors.ParameterException;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.EntityData;
import qdream.relay.types.NumberData;
import qdream.relay.types.BooleanData;
import qdream.relay.entities.StringDisplay;

/**
 * 为 StringDisplay 添加能量操作
 *
 * <h3>功能</h3>
 * <ul>
 * <li>从数据栈弹出实体引用和能量值</li>
 * <li>验证实体是否为 StringDisplay 类型</li>
 * <li>为实体添加指定能量</li>
 * </ul>
 *
 * <h3>能量公式</h3>
 * <pre>
 * 调用者消耗 = amount (添加的能量值)
 * 实体获得 = amount
 * </pre>
 *
 * 弹出：entity (实体引用), number (能量值)
 * 压入：boolean (是否成功添加)
 */
public class AddStringDisplayEnergy extends Instruction {

    public AddStringDisplayEnergy() {
        super("relay:add_string_display_energy", 1, 2, OperationSignature.builder()
                .consumesFromData("entity", "relay:entity")
                .consumesFromData("amount", "relay:number")
                .producesToData("success", "relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出参数
        EntityData entityData = StackHelpers.popEntity(executor, id);
        NumberData amountData = StackHelpers.popNumber(executor, id);

        // 获取实体对象
        var entity = entityData.getEntity();
        if (entity == null || !(entity instanceof StringDisplay display)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 验证能量值
        double amount = amountData.getValue();
        if (amount <= 0) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 检查调用者是否有足够能量
        if (!OperationHelpers.consumeEnergy(executor, amount)) {
            executor.pushData(new BooleanData(false));
            return;
        }

        // 为实体添加能量
        display.addEnergy(amount);

        // 压入成功标志
        executor.pushData(new BooleanData(true));
    }
}
