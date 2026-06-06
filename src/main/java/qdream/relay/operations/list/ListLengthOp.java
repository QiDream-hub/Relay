package qdream.relay.operations.list;

import qdream.relay.types.ProgramBlock;
import qdream.relay.types.NumberIota;

import java.util.List;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Spell;

/**
 * List Length 操作 - 获取列表长度
 * 输入：列表
 * 输出：数值（长度）
 */
public class ListLengthOp extends Spell {

    protected ListLengthOp() {
        super("relay:list_length", 1, OperationSignature.builder()
                .input("list")
                .output("number")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable listData = executor.popData();
        if (listData == null) return;
        if (!(listData instanceof ProgramBlock listBlock)) {
            executor.triggerMishap("操作 relay:list_length 期望 list 类型，实际为：" + listData.getId());
            return;
        }

        List<Executable> list = listBlock.getItems();
        executor.pushData(new NumberIota(list.size()));
    }

}
