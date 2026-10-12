package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 执行间隔超出有效范围
 * <p>翻译键：{@code error.interval_out_of_range}</p>
 */
public class IntervalOutOfRangeError extends ParameterException {

    public IntervalOutOfRangeError(StateMachine executor, int value, int min, int max) {
        super(executor, "error.interval_out_of_range", value, min, max);
    }
}
