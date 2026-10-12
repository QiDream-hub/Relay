package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 批量复制：计数必须大于 0
 * <p>翻译键：{@code error.batch_count_must_be_positive}</p>
 */
public class BatchCountMustBePositiveError extends ParameterException {

    public BatchCountMustBePositiveError(StateMachine executor) {
        super(executor, "error.batch_count_must_be_positive");
    }
}
