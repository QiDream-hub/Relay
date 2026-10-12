package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 批量复制：参数必须是数字
 * <p>翻译键：{@code error.batch_count_must_be_number}</p>
 */
public class BatchCountMustBeNumberError extends ParameterException {

    public BatchCountMustBeNumberError(StateMachine executor) {
        super(executor, "error.batch_count_must_be_number");
    }
}
