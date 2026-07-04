package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.base.OperationHelpers;
import qdream.relay.types.ListData;

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
        if (valueData == null) return;
        
        ListData list = OperationHelpers.popList(executor, "relay:list_append");
        if (list == null) return;

        // 创建新列表并添加原列表元素
        List<Executable> newList = list.getValue();
        newList.add(valueData);
        executor.pushData(new ListData(newList));
    }

}
