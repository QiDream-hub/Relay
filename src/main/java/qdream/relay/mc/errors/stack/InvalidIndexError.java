package qdream.relay.mc.errors.stack;

import qdream.relay.engine.StateMachine;

/**
 * 索引无效
 * <p>翻译键：{@code error.invalid_index}</p>
 */
public class InvalidIndexError extends StackException {

    public InvalidIndexError(StateMachine executor, int index) {
        super(executor, "error.invalid_index", index);
    }
}
