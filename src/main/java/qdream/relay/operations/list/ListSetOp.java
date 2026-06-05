package qdream.relay.operations.list;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * List Set 操作 - 设置列表指定索引的元素
 * 输入：列表，索引（数值），值
 * 输出：新列表
 */
public class ListSetOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota valueIota = executor.popData();
        Iota indexIota = executor.popData();
        Iota listIota = executor.popData();

        if (valueIota == null || indexIota == null || listIota == null) {
            throw new IllegalArgumentException("list_set 需要列表、索引和值参数");
        }

        if (!listIota.isList()) {
            throw new IllegalArgumentException("list_set 第一个参数需要是列表");
        }

        if (!indexIota.isNumber()) {
            throw new IllegalArgumentException("list_set 第二个参数需要是数值");
        }

        List<Iota> list = listIota.asList();
        int index = indexIota.asInt();

        if (index < 0 || index >= list.size()) {
            throw new IllegalArgumentException("list_set 索引超出范围：" + index);
        }

        // 创建新列表（不可变修改）
        List<Iota> newList = new ArrayList<>(list);
        newList.set(index, valueIota);
        
        executor.pushData(Iota.ofList(newList));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.LIST)
                .input(IotaType.NUMBER)
                .input(IotaType.ANY)
                .output(IotaType.LIST)
                .build();
    }

    @Override
    public int getCost() {
        return 2;
    }
}
