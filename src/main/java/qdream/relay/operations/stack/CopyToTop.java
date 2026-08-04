package qdream.relay.operations.stack;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.StackHelpers;
import qdream.relay.types.NumberData;

/**
 * 复制到栈顶操作 - 将指定位置的值复制一份到栈顶
 * 位置从 0 开始计数，0 表示栈顶，1 表示栈顶下一个元素
 */
public class CopyToTop extends Instruction {

    public CopyToTop() {
        super("relay:copy_to_top", 1, 0.05, OperationSignature.builder()
                .consumesFromData("index", "relay:number")
                .producesToData("copy", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData indexData = StackHelpers.popNumber(executor, id);
        int index = indexData.asInt();

        // 获取目标位置的元素并复制一份到栈顶
        var target = StackHelpers.getDataAt(executor, index, id);

        executor.pushData(target);
    }
}
