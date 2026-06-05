package qdream.relay.operations.list;
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
 * List Get 操作 - 获取列表指定索引的元素
 * 输入：列表，索引（数值）
 * 输出：元素或 null
 */
public class ListGetOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData indexData = executor.popData();
        if (!(indexData instanceof McIota indexIota)) return;
        IData listData = executor.popData();
        if (!(listData instanceof McIota listIota)) return;
        if (indexIota == null || listIota == null) {
            throw new IllegalArgumentException("list_get 需要列表和索引参数");
        }
        if (!listIota.isList()) {
            throw new IllegalArgumentException("list_get 第一个参数需要是列表");
        }
        if (!indexIota.isNumber()) {
            throw new IllegalArgumentException("list_get 第二个参数需要是数值");
        }
        List<IExecutable> list = listIota.asList();
        int index = indexIota.asInt();
        if (index < 0 || index >= list.size()) {
            executor.pushData(McIota.ofNull());
            return;
        }
        executor.pushData(list.get(index));
    }
    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("list")
                .input("number")
                .output("any")
                .build();
    }
    @Override
    public int getCost() {
        return 1;
    }
}
