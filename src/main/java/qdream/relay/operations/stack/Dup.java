package qdream.relay.operations.stack;

import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Dup 操作 - 复制数据栈顶部
 */
public class Dup extends Instruction {

    public Dup() {
        super("relay:dup", 1, 0.05, OperationSignature.builder()
                .consumesFromData("value", "any")
                .producesToData("copy", "any")
                .producesToData("original", "any")
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
