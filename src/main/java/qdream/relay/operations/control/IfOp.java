package qdream.relay.operations.control;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.BooleanData;
import qdream.relay.types.ListData;

/**
 * If 操作 - 条件分支
 * 从数据栈弹出：条件
 * 从程序栈弹出：真分支列表、假分支列表
 * 根据条件将其中一个分支压入程序栈
 */
public class IfOp extends Instruction {

    public IfOp() {
        super("relay:if", 1, 0.25, OperationSignature.builder()
                .consumesFromData("condition", "relay:boolean")
                .consumesFromProgram("trueBranch", "any")
                .consumesFromProgram("falseBranch", "any")
                .producesToProgram("selectedBranch", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        BooleanData condition = StackHelpers.popBoolean(executor, id);
        if (condition == null) return;
        
        Executable trueBranch = executor.popProgram();
        if (trueBranch == null) return;
        
        Executable falseBranch = executor.popProgram();
        if (falseBranch == null) return;

        // 根据条件选择分支
        Executable selected = condition.asBoolean()
                ? trueBranch
                : falseBranch;

        executor.pushProgram(selected);
    }

}
