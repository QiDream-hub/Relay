package qdream.relay.mc.errors.stack;

import qdream.relay.engine.StateMachine;

/**
 * 数据栈不足，需要指定数量的参数
 * <p>翻译键：{@code error.stack_insufficient}</p>
 */
public class StackInsufficientError extends StackException {

    public StackInsufficientError(StateMachine executor, int required) {
        super(executor, "error.stack_insufficient", required);
    }
}
