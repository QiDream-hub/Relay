package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NumberData;

/**
 * 复制到栈顶操作 - 将指定位置的值复制一份到栈顶
 * 位置从 0 开始计数，0 表示栈顶，1 表示栈顶下一个元素
 */
public class CopyToTopOp extends Spell {

    public CopyToTopOp() {
        super("relay:copy_to_top", 1, 1, OperationSignature.builder()
                .consumesFromData("index", "relay:number")
                .producesToData("copy", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData indexData = OperationHelpers.popNumber(executor, id);
        if (indexData == null) {
            return;
        }

        int index = indexData.asInt();

        // 获取目标位置的元素并复制一份到栈顶
        var target = OperationHelpers.getDataAt(executor, index, id);
        if (target == null) {
            return;
        }

        executor.pushData(target);
    }
}
