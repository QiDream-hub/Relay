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
 * 输入：列表，索引（数值）
 * 输出：元素或 null
 */
public class ListGetOp extends Spell {

    public ListGetOp() {
        super("relay:list_get", 1, 1, OperationSignature.builder()
                .consumesFromData("list", "relay:list")
                .consumesFromData("index", "relay:number")
                .producesToData("element", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData index = OperationHelpers.popNumber(executor, "relay:list_get");
        if (index == null) return;
        
        ListData list = OperationHelpers.popList(executor, "relay:list_get");
        if (list == null) return;

        List<Executable> value = list.getValue();
        int idx = (int) index.asDouble();
        if (idx < 0 || idx >= value.size()) {
            executor.pushData(NullData.INSTANCE);
            return;
        }
        executor.pushData(value.get(idx));
    }

}
