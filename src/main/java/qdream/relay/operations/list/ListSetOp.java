package qdream.relay.operations.list;

import qdream.relay.types.ProgramBlock;
import qdream.relay.types.NumberIota;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Spell;

import java.util.List;
import java.util.ArrayList;

/**
 * List Set 操作 - 设置列表指定索引的元素
 * 输入：列表，索引（数值），值
 * 输出：新列表
 */
public class ListSetOp extends Spell {

    protected ListSetOp() {
        super("relay:list_set", 2, OperationSignature.builder()
                .input("list")
                .input("number")
                .input("any")
                .output("list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable valueData = executor.popData();
        if (valueData == null) return;
        Executable indexData = executor.popData();
        if (indexData == null) return;
        if (!(indexData instanceof NumberIota index)) {
            executor.triggerMishap("操作 relay:list_set 期望 number 类型，实际为：" + indexData.getId());
            return;
        }
        Executable listData = executor.popData();
        if (listData == null) return;
        if (!(listData instanceof ProgramBlock listBlock)) {
            executor.triggerMishap("操作 relay:list_set 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> list = listBlock.getItems();
        int indexVal = index.asInt();
        if (indexVal < 0 || indexVal >= list.size()) {
            executor.triggerMishap("操作 relay:list_set 索引超出范围：" + indexVal);
            return;
        }
        // 创建新列表（不可变修改）
        List<Executable> newList = new ArrayList<>(list);
        newList.set(indexVal, valueData);
        executor.pushData(new ProgramBlock(newList));
    }

}
