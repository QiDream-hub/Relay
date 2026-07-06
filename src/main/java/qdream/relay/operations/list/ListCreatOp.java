package qdream.relay.operations.list;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.ListData;
import qdream.relay.types.NumberData;

import java.util.ArrayList;
import java.util.List;

/**
 * List Create 操作 - 创建列表
 * 输入：size (数值), elements (任意数量的元素)
 * 输出：新列表
 */
public class ListCreatOp extends Spell {
    public ListCreatOp() {
        super("relay:list_creat", 1, 1, OperationSignature.builder()
                .consumesFromData("size", "relay:number")
                .consumesFromData("elements", "...any")
                .producesToData("result", "relay:list")
                .build());
    }
    
    @Override
    public void execute(StateMachine executor) {
        NumberData sizeData = OperationHelpers.popNumber(executor, id);
        if (sizeData == null) return;
        
        int size = (int) sizeData.asDouble();
        if (executor.getDataStackSize() < size) {
            executor.triggerMishap("操作 relay:list_creat 期望数据栈大小为：" + size + "，实际为：" + executor.getDataStackSize());
            return;
        }
        
        List<Executable> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(executor.popData());
        }
        executor.pushData(new ListData(list));
    }
}
