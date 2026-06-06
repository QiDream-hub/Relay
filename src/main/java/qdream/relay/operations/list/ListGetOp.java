package qdream.relay.operations.list;

import qdream.relay.types.ProgramBlock;
import qdream.relay.types.NumberIota;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Spell;

import qdream.relay.types.NullIota;

import java.util.List;

/**
 * List Get 操作 - 获取列表指定索引的元素
 * 输入：列表，索引（数值）
 * 输出：元素或 null
 */
public class ListGetOp extends Spell {

    protected ListGetOp() {
        super("relay:list_get", 1, OperationSignature.builder()
                .input("list")
                .input("number")
                .output("any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable indexData = executor.popData();
        if (indexData == null) return;
        if (!(indexData instanceof NumberIota index)) {
            executor.triggerMishap("操作 relay:list_get 期望 number 类型，实际为：" + indexData.getId());
            return;
        }
        Executable listData = executor.popData();
        if (listData == null) return;
        if (!(listData instanceof ProgramBlock listBlock)) {
            executor.triggerMishap("操作 relay:list_get 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> list = listBlock.getItems();
        int indexVal = index.asInt();
        if (indexVal < 0 || indexVal >= list.size()) {
            executor.pushData(NullIota.INSTANCE);
            return;
        }
        executor.pushData(list.get(indexVal));
    }

}
