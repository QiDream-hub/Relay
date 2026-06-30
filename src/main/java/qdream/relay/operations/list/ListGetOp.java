package qdream.relay.operations.list;

import qdream.relay.types.NumberType;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.ListType;
import qdream.relay.types.NullType;

import java.util.List;

/**
 * List Get 操作 - 获取列表指定索引的元素
 * 输入：列表，索引（数值）
 * 输出：元素或 null
 */
public class ListGetOp extends Spell {

    public ListGetOp() {
        super("relay:list_get", 1, 1, OperationSignature.builder()
                .consumesFromData("relay:list")
                .consumesFromData("number")
                .producesToData("any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation indexData = (Operation) executor.popData();
        if (indexData == null)
            return;
        if (!(indexData instanceof NumberType index)) {
            executor.triggerMishap("操作 relay:list_get 期望 number 类型，实际为：" + indexData.getId());
            return;
        }
        Operation listData = (Operation) executor.popData();
        if (listData == null)
            return;
        if (!(listData instanceof ListType list)) {
            executor.triggerMishap("操作 relay:list_get 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> value = list.getValue();
        int idx = (int) index.asDouble();
        if (idx < 0 || idx >= value.size()) {
            executor.pushData(NullType.INSTANCE);
            return;
        }
        executor.pushData(value.get(idx));
    }

}
