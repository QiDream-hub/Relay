package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.ListData;

import java.util.List;
import java.util.ArrayList;

/**
 * List Uniq 操作 - 移除列表中的重复元素
 * 输入：列表
 * 输出：去重后的新列表
 */
public class ListUniqOp extends qdream.relay.mc.base.Instruction {

    public ListUniqOp() {
        super("relay:list_uniq", 2, 0.25, OperationSignature.builder()
                .consumesFromData("list", "relay:list")
                .producesToData("result", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        ListData list = StackHelpers.popList(executor, id);
        if (list == null) return;

        List<Executable> originalList = list.getValue();
        List<Executable> uniqueList = new ArrayList<>();

        // 遍历原列表，只添加不重复的元素
        for (Executable item : originalList) {
            boolean isDuplicate = false;
            for (Executable uniqueItem : uniqueList) {
                if (item instanceof Operation op1 && uniqueItem instanceof Operation op2) {
                    if (op1.equalsTo(op2)) {
                        isDuplicate = true;
                        break;
                    }
                } else if (item.equals(uniqueItem)) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                uniqueList.add(item);
            }
        }

        executor.pushData(new ListData(uniqueList));
    }
}
