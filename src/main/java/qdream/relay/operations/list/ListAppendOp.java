package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.ListType;

import java.util.List;
import java.util.ArrayList;

/**
 * List Append 操作 - 在列表末尾添加元素
 * 输入：列表，值
 * 输出：新列表
 */
public class ListAppendOp extends Spell {

    public ListAppendOp() {
        super("relay:list_append", 2, 1, OperationSignature.builder()
                .consumesFromData("list", "relay:list")
                .consumesFromData("element", "any")
                .producesToData("result", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable valueData = executor.popData();
        if (valueData == null)
            return;
        Operation listData = (Operation) executor.popData();
        if (listData == null)
            return;
        if (!(listData instanceof ListType list)) {
            executor.triggerMishap("操作 relay:list_append 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        // 创建新列表并添加原列表元素
        List<Executable> newList = list.getValue();
        newList.add(valueData);
        executor.pushData(new ListType(newList));
    }

}
