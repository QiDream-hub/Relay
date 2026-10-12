package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 参数超出有效范围
 * <p>翻译键：{@code error.parameter_out_of_range}</p>
 */
public class ParameterOutOfRangeError extends ParameterException {

    public ParameterOutOfRangeError(StateMachine executor, Object value) {
        super(executor, "error.parameter_out_of_range", value);
    }
}
