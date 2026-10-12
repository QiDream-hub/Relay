package qdream.relay.mc.errors.type;

import qdream.relay.engine.StateMachine;

/**
 * 无效的参数
 * <p>翻译键：{@code error.parameter_invalid}</p>
 */
public class ParameterInvalidError extends TypeException {

    public ParameterInvalidError(StateMachine executor, Object parameter) {
        super(executor, "error.parameter_invalid", parameter);
    }
}
