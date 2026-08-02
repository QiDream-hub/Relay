package qdream.relay.operations.control;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.signature.OperationSignature;

/**
 * Stop 操作 - 强制终止程序
 * 清空程序栈和数据栈
 */
public class Stop extends Instruction {

    public Stop() {
        super("relay:stop", 1, 1, OperationSignature.builder().build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 清空双栈，终止程序执行
        executor.clear();
    }

}
