package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;

import java.util.List;
import java.util.ArrayList;

/**
 * List Remove 操作 - 移除列表指定索引的元素
 * 输入：列表（栈顶 +1），索引（栈顶）
 * 输出：新列表（移除元素后的副本）、被移除的元素
 */
public class ListRemoveOp extends Spell {

    public ListRemoveOp() {
        super("relay:list_remove", 1, 1, OperationSignature.builder()
                .consumesFromData("index", "relay:number")
                .consumesFromData("list", "relay:list")
                .producesToData("result", "relay:list")
                .producesToData("removed", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出并消耗索引
        NumberData index = OperationHelpers.popNumber(executor, id);
        if (index == null)
            return;

        // 栈顶是索引，栈顶 +1 是列表
        ListData list = OperationHelpers.popList(executor, id);
        if (list == null)
            return;

        List<Executable> listData = list.getValue();
        int idx = (int) index.asDouble();
        if (idx < 0 || idx >= listData.size()) {
            executor.triggerMishap("relay:list_remove 索引超出范围：" + idx);
            return;
        }

        // 创建新列表（不可变修改）
        List<Executable> newList = new ArrayList<>(listData);
        Executable removed = newList.remove(idx);

        // 将新列表和被移除的元素压入栈
        executor.pushData(new ListData(newList));
        executor.pushData(removed);
    }

}
