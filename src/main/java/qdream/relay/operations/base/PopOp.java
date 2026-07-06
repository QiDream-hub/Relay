package qdream.relay.operations.base;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Pop 操作 - 弹出数据栈顶部
 */
public class PopOp extends Spell {

    public PopOp() {
        super("relay:pop", 1, 0.25, OperationSignature.builder()
                .consumesFromData("value", "any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        executor.popData();
    }

}
