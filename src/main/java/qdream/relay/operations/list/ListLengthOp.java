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
 * List Length 操作 - 获取列表长度
 * 输入：列表
 * 输出：数值（长度）
 */
public class ListLengthOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        IData listData = executor.popData();
        if (!(listData instanceof McIota listIota)) return;
        if (listIota == null) {
            throw new IllegalArgumentException("list_length 需要列表参数");
        }
        if (!listIota.isList()) {
            throw new IllegalArgumentException("list_length 需要列表参数，得到：" + listIota.getType());
        }
        List<IExecutable> list = listIota.asList();
        executor.pushData(McIota.ofInt(list.size()));
    }
    @Override
    public OperationSignature getSignature() {
        return OperationSignature.builder()
                .input("list")
                .output("number")
                .build();
    }
    @Override
    public int getCost() {
        return 1;
    }
}
