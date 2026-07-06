package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.operations.OperationHelpers;
import qdream.relay.types.NumberData;

/**
 * 移到栈顶操作 - 将指定位置的值移到栈顶
 * 位置从 0 开始计数，0 表示栈顶，1 表示栈顶下一个元素
 */
public class MoveToTopOp extends Spell {

    public MoveToTopOp() {
        super("relay:move_to_top", 1, 0.25, OperationSignature.builder()
                .consumesFromData("index", "relay:number")
                .producesToData("value", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        NumberData indexData = OperationHelpers.popNumber(executor, id);
        if (indexData == null) {
            return;
        }

        int index = indexData.asInt();

        // 移除目标位置的元素并压入栈顶
        var target = OperationHelpers.removeDataAt(executor, index, id);
        if (target == null) {
            return;
        }

        executor.pushData(target);
    }
}
