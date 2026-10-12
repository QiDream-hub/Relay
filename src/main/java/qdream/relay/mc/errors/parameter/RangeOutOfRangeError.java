package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 交互范围超出有效范围
 * <p>翻译键：{@code error.range_out_of_range}</p>
 */
public class RangeOutOfRangeError extends ParameterException {

    public RangeOutOfRangeError(StateMachine executor, int value, int min, int max) {
        super(executor, "error.range_out_of_range", value, min, max);
    }
}
