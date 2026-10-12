package qdream.relay.mc.errors.stack;

import qdream.relay.engine.StateMachine;

/**
 * 列表索引超出范围
 * <p>翻译键：{@code error.list_index_out_of_bounds}</p>
 */
public class ListIndexOutOfBoundsError extends StackException {

    public ListIndexOutOfBoundsError(StateMachine executor, int index) {
        super(executor, "error.list_index_out_of_bounds", index);
    }
}
