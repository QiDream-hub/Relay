package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.ListData;
import qdream.relay.types.NullData;
import qdream.relay.types.NumberData;

import java.util.List;

/**
 * List Get 操作 - 获取列表指定索引的元素
 * 使用 peek 方式读取列表（不消耗），只消耗索引
 * 输入：列表（栈顶 +1），索引（栈顶）
 * 输出：元素或 null
 */
public class ListGetOp extends Spell {

    public ListGetOp() {
        super("relay:list_get", 1, 0.25, OperationSignature.builder()
                .consumesFromData("index", "relay:number")
                .consumesFromData("list", "relay:list")
                .producesToData("element", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {

        // 弹出并消耗索引
        NumberData index = OperationHelpers.popNumber(executor, id);
        if (index == null)
            return;

        // 栈顶是索引，栈顶 +1 是列表
        // 使用 peek 读取列表（不消耗）
        ListData list = OperationHelpers.peekList(executor, id);
        if (list == null)
            return;

        List<Executable> value = list.getValue();
        int idx = (int) index.asDouble();
        if (idx < 0 || idx >= value.size()) {
            executor.pushData(NullData.INSTANCE);
            return;
        }
        // 将元素压入栈（列表仍然保留在栈顶 +1 位置）
        executor.pushData(value.get(idx));
    }

}
