package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 栈重排：索引必须是数字
 * <p>翻译键：{@code error.stack_rearrange_index_must_be_number}</p>
 */
public class StackRearrangeIndexMustBeNumberError extends ParameterException {

    public StackRearrangeIndexMustBeNumberError(StateMachine executor) {
        super(executor, "error.stack_rearrange_index_must_be_number");
    }
}
