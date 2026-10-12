package qdream.relay.mc.errors.stack;

import qdream.relay.engine.StateMachine;

/**
 * 批量复制：计数超出栈大小
 * <p>翻译键：{@code error.batch_count_exceeds_stack}</p>
 */
public class BatchCountExceedsStackError extends StackException {

    public BatchCountExceedsStackError(StateMachine executor) {
        super(executor, "error.batch_count_exceeds_stack");
    }
}
