package qdream.relay.mc.errors.stack;

import qdream.relay.engine.StateMachine;

/**
 * 索引超出范围
 * <p>翻译键：{@code error.index_out_of_bounds}</p>
 */
public class IndexOutOfBoundsError extends StackException {

    public IndexOutOfBoundsError(StateMachine executor, int index, int size) {
        super(executor, "error.index_out_of_bounds", index, size);
    }
}
