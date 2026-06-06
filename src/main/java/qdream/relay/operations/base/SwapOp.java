package qdream.relay.operations.base;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Spell;

/**
 * Swap 操作 - 交换数据栈顶部两个元素
 */
public class SwapOp extends Spell {

    protected SwapOp() {
        super("relay:swap", 1, OperationSignature.builder()
                .input("any")
                .input("any")
                .output("any")
                .output("any")
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
