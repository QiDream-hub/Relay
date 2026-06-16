package qdream.relay.operations.list;

import qdream.relay.types.ListIota;
import qdream.relay.types.NumberIota;

import java.util.List;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * List Length 操作 - 获取列表长度
 * 输入：列表
 * 输出：数值（长度）
 */
public class ListLengthOp extends Spell {

    public ListLengthOp() {
        super("relay:list_length", 1, 1, OperationSignature.builder()
                .input("relay:list")
                .output("relay:number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Operation listData = (Operation) executor.popData();
        if (listData == null)
            return;
        if (!(listData instanceof ListIota listBlock)) {
            executor.triggerMishap("操作 relay:list_length 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> list = listBlock.getValue();
        executor.pushData(new NumberIota(list.size()));
    }

}
