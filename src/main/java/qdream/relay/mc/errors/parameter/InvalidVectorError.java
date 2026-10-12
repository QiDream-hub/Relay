package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 无效的向量（NaN 或 Infinity）
 * <p>翻译键：{@code error.invalid_vector}</p>
 */
public class InvalidVectorError extends ParameterException {

    public InvalidVectorError(StateMachine executor) {
        super(executor, "error.invalid_vector");
    }
}
