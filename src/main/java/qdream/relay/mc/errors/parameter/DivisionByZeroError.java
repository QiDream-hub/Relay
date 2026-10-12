package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 除零错误
 * <p>翻译键：{@code error.division_by_zero}</p>
 */
public class DivisionByZeroError extends ParameterException {

    public DivisionByZeroError(StateMachine executor) {
        super(executor, "error.division_by_zero");
    }
}
