package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.ListData;

import java.util.List;

/**
 * List Unpack 操作 - 将列表解包到数据栈中
 * 输入：列表
 * 输出：多个值（列表中的每个元素）
 */
public class ListUnpackOp extends Instruction {

    public ListUnpackOp() {
        super("relay:list_unpack", 2, 0.25, OperationSignature.builder()
                .consumesFromData("list", "relay:list")
                .producesToData("elements", "...any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        ListData list = StackHelpers.popList(executor, id);
        if (list == null) return;

        List<Executable> listData = list.getValue();
        // 将列表中的每个元素依次压入数据栈
        for (Executable item : listData) {
            executor.pushData(item);
        }
    }

}
