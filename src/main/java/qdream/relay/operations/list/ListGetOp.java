package qdream.relay.operations.list;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

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
        Iota indexIota = executor.popData();
        Iota listIota = executor.popData();

        if (indexIota == null || listIota == null) {
            throw new IllegalArgumentException("list_get 需要列表和索引参数");
        }

        if (!listIota.isList()) {
            throw new IllegalArgumentException("list_get 第一个参数需要是列表");
        }

        if (!indexIota.isNumber()) {
            throw new IllegalArgumentException("list_get 第二个参数需要是数值");
        }

        List<Iota> list = listIota.asList();
        int index = indexIota.asInt();

        if (index < 0 || index >= list.size()) {
            executor.pushData(Iota.ofNull());
            return;
        }

        executor.pushData(list.get(index));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.LIST)
                .input(IotaType.NUMBER)
                .output(IotaType.ANY)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
