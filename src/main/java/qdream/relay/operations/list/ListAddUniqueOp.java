package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.ListData;

import java.util.ArrayList;
import java.util.List;

/**
 * List Add Unique 操作 - 向列表添加元素（自动去重）
 * 输入：列表，值
 * 输出：新列表（如果元素已存在则不添加）
 */
public class ListAddUniqueOp extends Spell {

    public ListAddUniqueOp() {
        super("relay:list_add_unique", 2, 0.25, OperationSignature.builder()
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

        // 创建新列表（保持不可变性）
        List<Executable> newList = new ArrayList<>(list.getValue());

        // 检查是否已存在相同元素
        boolean isDuplicate = false;
        for (Executable existingItem : newList) {
            if (valueData instanceof Operation op1 && existingItem instanceof Operation op2) {
                if (op1.equalsTo(op2)) {
                    isDuplicate = true;
                    break;
                }
            } else if (valueData.equals(existingItem)) {
                isDuplicate = true;
                break;
            }
        }

        // 如果不存在才添加
        if (!isDuplicate) {
            newList.add(valueData);
        }

        executor.pushData(new ListData(newList));
    }
}
