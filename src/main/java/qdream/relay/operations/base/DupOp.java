package qdream.relay.operations.base;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.base.Spell;

/**
 * Dup 操作 - 复制数据栈顶部
 */
public class DupOp extends Spell {

    public DupOp() {
        super("relay:dup", 1, 1, OperationSignature.builder()
                .input("any")
                .output("any")
                .output("any")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        Executable topData = executor.popData();
        if (topData != null) {
            executor.pushData(topData);
            executor.pushData(topData);
        }
    }

}
