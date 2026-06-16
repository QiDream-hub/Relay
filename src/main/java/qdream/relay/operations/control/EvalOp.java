package qdream.relay.operations.control;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.ListIota;

import java.util.List;

/**
 * Eval 操作 - 将列表反转后压入程序栈
 * 实现控制流的核心操作
 */
public class EvalOp extends Spell {

    public EvalOp() {
        super("relay:eval", 1, 1, OperationSignature.builder()
                .input("relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation listData = (Operation) executor.popData();
        if (listData == null)
            return;
        if (!(listData instanceof ListIota list)) {
            executor.triggerMishap("操作 relay:eval 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> reversed = list.getValue();
        executor.loadProgram(reversed);
    }

}
