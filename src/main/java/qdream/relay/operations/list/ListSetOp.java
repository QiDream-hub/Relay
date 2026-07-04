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
 * List Set 操作 - 设置列表指定索引的元素
 * 输入：列表，索引（数值），值
 * 输出：新列表
 */
public class ListSetOp extends Spell {

    public ListSetOp() {
        super("relay:list_set", 1, 1, OperationSignature.builder()
                .consumesFromData("list", "relay:list")
                .consumesFromData("index", "relay:number")
                .consumesFromData("element", "any")
                .producesToData("result", "relay:list")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable valueData = executor.popData();
        if (valueData == null) return;
        
        NumberData index = OperationHelpers.popNumber(executor, "relay:list_set");
        if (index == null) return;
        
        ListData list = OperationHelpers.popList(executor, "relay:list_set");
        if (list == null) return;

        List<Executable> listData = list.getValue();
        int indexVal = index.asInt();
        if (indexVal < 0 || indexVal >= listData.size()) {
            executor.triggerMishap("操作 relay:list_set 索引超出范围：" + indexVal);
            return;
        }
        // 创建新列表（不可变修改）
        List<Executable> newList = new ArrayList<>(listData);
        newList.set(indexVal, valueData);
        executor.pushData(new ListData(newList));
    }

}
