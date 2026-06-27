package qdream.relay.operations.control;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.BooleanIota;

/**
 * If 操作 - 条件分支
 * 从数据栈弹出：条件、真分支列表、假分支列表
 * 根据条件将其中一个分支反转后压入程序栈
 */
public class IfOp extends Spell {

    public IfOp() {
        super("relay:if", 1, 1, OperationSignature.builder()
                .input("boolean")
                .input("any")
                .input("any")
                .output("any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {

        Operation conditionData = (Operation) executor.popData();
        if (conditionData == null)
            return;
        if (!(conditionData instanceof BooleanIota condition)) {
            executor.triggerMishap("操作 relay:if 期望 boolean 类型，实际为：" + conditionData.getId());
            return;
        }
        Operation trueBranchData = (Operation) executor.popProgram();
        if (trueBranchData == null)
            return;
        Operation falseBranchData = (Operation) executor.popProgram();
        if (falseBranchData == null)
            return;

        // 根据条件选择分支
        Executable selected = condition.asBoolean()
                ? trueBranchData
                : falseBranchData;

        executor.pushProgram(selected);
    }

}
