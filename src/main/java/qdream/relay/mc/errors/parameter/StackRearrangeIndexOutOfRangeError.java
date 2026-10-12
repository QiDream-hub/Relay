package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 栈重排：索引超出范围
 * <p>翻译键：{@code error.stack_rearrange_index_out_of_range}</p>
 */
public class StackRearrangeIndexOutOfRangeError extends ParameterException {

    public StackRearrangeIndexOutOfRangeError(StateMachine executor, int index, int max) {
        super(executor, "error.stack_rearrange_index_out_of_range", index, max);
    }
}
