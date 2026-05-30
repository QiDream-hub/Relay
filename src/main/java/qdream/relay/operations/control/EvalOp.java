package qdream.relay.operations.control;

import qdream.relay.core.Iota;
import qdream.relay.core.OperationSignature;
import qdream.relay.core.IotaType;
import qdream.relay.core.StackOperation;
import qdream.relay.core.StateMachine;

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
        Iota listIota = executor.popData();
        
        if (listIota == null) {
            return;
        }
        
        if (!listIota.isList()) {
            throw new IllegalArgumentException("Eval 需要一个列表参数");
        }
        
        List<Iota> list = listIota.asList();
        List<Iota> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        
        for (Iota iota : reversed) {
            executor.pushProgram(iota);
        }
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.LIST)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
