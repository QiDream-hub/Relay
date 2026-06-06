package qdream.relay.operations.list;

import qdream.relay.types.ProgramBlock;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Spell;

import java.util.List;
import java.util.ArrayList;

/**
 * List Append 操作 - 在列表末尾添加元素
 * 输入：列表，值
 * 输出：新列表
 */
public class ListAppendOp extends Spell {

    protected ListAppendOp() {
        super("relay:list_append", 2, OperationSignature.builder()
                .input("list")
                .input("any")
                .output("list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable valueData = executor.popData();
        if (valueData == null) return;
        Executable listData = executor.popData();
        if (listData == null) return;
        if (!(listData instanceof ProgramBlock listBlock)) {
            executor.triggerMishap("操作 relay:list_append 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> list = listBlock.getItems();
        // 创建新列表（不可变修改）
        List<Executable> newList = new ArrayList<>(list);
        newList.add(valueData);
        executor.pushData(new ProgramBlock(newList));
    }

}
