package qdream.relay.operations.list;
import qdream.relay.engine.IExecutable;
import qdream.relay.mc.McIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IData;
import qdream.relay.engine.IExecutable;

import java.util.List;
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
        IData valueData = executor.popData();
        if (!(valueData instanceof McIota valueIota)) return;
        IData listData = executor.popData();
        if (!(listData instanceof McIota listIota)) return;
        if (valueIota == null || listIota == null) {
            throw new IllegalArgumentException("list_append 需要列表和值参数");
        }
        if (!listIota.isList()) {
            throw new IllegalArgumentException("list_append 第一个参数需要是列表");
        }
        List<IExecutable> list = listIota.asList();
        // 创建新列表（不可变修改）
        List<IExecutable> newList = new ArrayList<>(list);
        newList.add(valueIota);
        executor.pushData(McIota.ofList(newList));
    }
    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("list")
                .input("any")
                .output("list")
                .build();
    }
    @Override
    public int getCost() {
        return 2;
    }
}
