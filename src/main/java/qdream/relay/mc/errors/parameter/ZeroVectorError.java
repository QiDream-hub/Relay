package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 向量不能为零向量
 * <p>翻译键：{@code error.zero_vector}</p>
 */
public class ZeroVectorError extends ParameterException {

    public ZeroVectorError(StateMachine executor) {
        super(executor, "error.zero_vector");
    }
}
