package qdream.relay.operations.control;

import qdream.relay.engine.IExecutable;
import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;

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
        IData listData = executor.popData();
        if (!(listData instanceof McIota listIota)) return;
        
        if (listIota == null) {
            return;
        }
        
        if (!listIota.isList()) {
            throw new IllegalArgumentException("Eval 需要一个列表参数");
        }
        
        List<IExecutable> list = listIota.asList();
        List<IExecutable> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);

        for (IExecutable iota : reversed) {
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
