package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;

import java.util.List;

/**
 * List Length 操作 - 获取列表长度
 * 输入：列表
 * 输出：数值（长度）
 */
public class ListLengthOp extends Instruction {

    public ListLengthOp() {
        super("relay:list_length", 1, 0.25, OperationSignature.builder()
                .consumesFromData("list", "relay:list")
                .producesToData("size", "relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        ListData list = StackHelpers.popList(executor, id);
        if (list == null) return;

        List<Executable> listData = list.getValue();
        executor.pushData(new NumberData(listData.size()));
    }

}
