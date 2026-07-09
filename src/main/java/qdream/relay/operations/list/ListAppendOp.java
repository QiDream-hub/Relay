package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.ListData;

import java.util.ArrayList;
import java.util.List;

/**
 * List Append 操作 - 在列表末尾添加元素
 * 输入：列表，值
 * 输出：新列表
 */
public class ListAppendOp extends Spell {

    public ListAppendOp() {
        super("relay:list_append", 1, 0.25, OperationSignature.builder()
                .consumesFromData("element", "any")
                .consumesFromData("list", "relay:list")
                .producesToData("result", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable valueData = OperationHelpers.popAny(executor);
        if (valueData == null)
            return;

        ListData list = OperationHelpers.popList(executor, id);
        if (list == null)
            return;

        // 创建新列表，复制原列表元素，然后添加新元素（保持不可变性）
        List<Executable> newList = new ArrayList<>(list.getValue());
        newList.add(valueData);
        executor.pushData(new ListData(newList));
    }

}
