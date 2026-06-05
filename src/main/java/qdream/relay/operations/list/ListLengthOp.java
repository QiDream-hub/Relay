package qdream.relay.operations.list;

import qdream.relay.engine.Iota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.IotaType;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * List Length 操作 - 获取列表长度
 * 输入：列表
 * 输出：数值（长度）
 */
public class ListLengthOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Iota listIota = executor.popData();

        if (listIota == null) {
            throw new IllegalArgumentException("list_length 需要列表参数");
        }

        if (!listIota.isList()) {
            throw new IllegalArgumentException("list_length 需要列表参数，得到：" + listIota.getType());
        }

        List<Iota> list = listIota.asList();
        executor.pushData(Iota.ofInt(list.size()));
    }

    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input(IotaType.LIST)
                .output(IotaType.NUMBER)
                .build();
    }

    @Override
    public int getCost() {
        return 1;
    }
}
