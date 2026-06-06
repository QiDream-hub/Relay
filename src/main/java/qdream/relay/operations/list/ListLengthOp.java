package qdream.relay.operations.list;

import qdream.relay.types.ProgramBlock;
import qdream.relay.types.NumberIota;
import qdream.relay.engine.OperationSignature;
import qdream.relay.engine.StackOperation;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.engine.Executable;

import java.util.List;

/**
 * List Length 操作 - 获取列表长度
 * 输入：列表
 * 输出：数值（长度）
 */
public class ListLengthOp implements StackOperation {
    @Override
    public void execute(StateMachine executor) {
        Executable listData = executor.popData();
        if (listData == null) return;
        if (!(listData instanceof ProgramBlock listBlock)) {
            executor.triggerMishap("操作 relay:list_length 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> list = listBlock.getItems();
        executor.pushData(new NumberIota(list.size()));
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
