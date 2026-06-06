package qdream.relay.operations.control;

import qdream.relay.engine.Executable;
import qdream.relay.types.ProgramBlock;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Eval 操作 - 将列表反转后压入程序栈
 * 实现控制流的核心操作
 */
public class EvalOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable listData = executor.popData();
        if (listData == null) return;
        if (!(listData instanceof ProgramBlock listBlock)) {
            executor.triggerMishap("操作 relay:eval 期望 list 类型，实际为：" + listData.getType());
            return;
        }

        List<Executable> list = listBlock.getItems();
        List<Executable> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);

        for (Executable iota : reversed) {
            executor.pushProgram(iota);
        }
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("list")
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
