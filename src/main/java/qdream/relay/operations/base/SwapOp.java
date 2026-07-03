package qdream.relay.operations.base;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Swap 操作 - 交换数据栈顶部两个元素
 */
public class SwapOp extends Spell {

    public SwapOp() {
        super("relay:swap", 1, 1, OperationSignature.builder()
                .consumesFromData("first", "any")
                .consumesFromData("second", "any")
                .producesToData("second", "any")
                .producesToData("first", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable topData = executor.popData();
        if (topData == null)
            return;
        Executable secondData = executor.popData();
        if (secondData == null)
            return;

        executor.pushData(topData);
        executor.pushData(secondData);
    }

}
