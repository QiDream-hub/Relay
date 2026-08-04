package qdream.relay.engine;

import qdream.relay.mc.errors.ExecutionException;

/**
 * 状态机警告异常
 * <p>
 * 用于状态机层面的警告，如程序已运行完成、空状态机操作等
 * 此异常会被 StateMachine.step() 捕获并触发 mishap
 */
public class Warning extends ExecutionException {
    public Warning(StateMachine state, String message) {
        super(message);
    }

    public Warning(StateMachine state, String message, Throwable cause) {
        super(message, cause);
    }
}
