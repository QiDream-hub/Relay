package qdream.relay.operations.list;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * List Append 操作 - 在列表末尾添加元素
 * 输入：列表，值
 * 输出：新列表
 */
public class ListAppendOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota valueIota = executor.popData();
        Iota listIota = executor.popData();

        if (valueIota == null || listIota == null) {
            throw new IllegalArgumentException("list_append 需要列表和值参数");
        }

        if (!listIota.isList()) {
            throw new IllegalArgumentException("list_append 第一个参数需要是列表");
        }

        List<Iota> list = listIota.asList();
        
        // 创建新列表（不可变修改）
        List<Iota> newList = new ArrayList<>(list);
        newList.add(valueIota);
        
        executor.pushData(Iota.ofList(newList));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.LIST)
                .input(IotaType.ANY)
                .output(IotaType.LIST)
                .build();
    }

    @Override
    public int getCost() {
        return 2;
    }
}
