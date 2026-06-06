package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Spell;

/**
 * Pop 操作 - 弹出数据栈顶部
 */
public class PopOp extends Spell {

    public PopOp() {
        super("relay:pop", 1, OperationSignature.builder()
                .input("any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        executor.popData();
    }

}
