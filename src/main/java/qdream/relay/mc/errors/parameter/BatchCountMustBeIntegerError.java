package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 批量复制：计数必须是整数
 * <p>翻译键：{@code error.batch_count_must_be_integer}</p>
 */
public class BatchCountMustBeIntegerError extends ParameterException {

    public BatchCountMustBeIntegerError(StateMachine executor) {
        super(executor, "error.batch_count_must_be_integer");
    }
}
