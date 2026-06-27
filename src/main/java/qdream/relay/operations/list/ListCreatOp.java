package qdream.relay.operations.list;

import java.util.ArrayList;
import java.util.List;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.types.ListType;
import qdream.relay.types.NumberType;

public class ListCreatOp extends Spell{
    public ListCreatOp() {
        super("relay:list_creat", 1, 1, OperationSignature.builder()
                .input("Number")
                .output("relay:list")
                .build());
    }
    @Override
    public void execute(StateMachine executor) {
        Operation numberIota = (Operation) executor.popData();
        if (numberIota == null)
            return;
        if (!(numberIota instanceof NumberType index)) {
            executor.triggerMishap("操作 relay:list_creat 期望 number 类型，实际为：" + numberIota.getId());
            return;
        }
        int size = executor.getDataStackSize();
        if (size < index.getValue()) {
            executor.triggerMishap("操作 relay:list_creat 期望数据栈大小为：" + index.getValue() + "，实际为：" + size);
        }
        List<Executable> list = new ArrayList<>();
        for (int i = 0; i < index.getValue(); i++) {
            list.add(executor.popData());
        }
        executor.pushData(new ListType(list));
    }

}
