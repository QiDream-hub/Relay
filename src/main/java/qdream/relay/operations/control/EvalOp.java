package qdream.relay.operations.control;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.ListData;

import java.util.List;

/**
 * Eval 操作 - 将列表反转后压入程序栈
 * 实现控制流的核心操作
 */
public class EvalOp extends Spell {

    public EvalOp() {
        super("relay:eval", 1, 1, OperationSignature.builder()
                .consumesFromData("program", "relay:list")
                .producesToProgram("instructions", "...any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        ListData list = OperationHelpers.popList(executor, "relay:eval");
        if (list == null) return;

        List<Executable> reversed = list.getValue();
        executor.loadProgram(reversed);
    }

}
