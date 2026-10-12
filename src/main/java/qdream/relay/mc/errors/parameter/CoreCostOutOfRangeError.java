package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 核心数量超出有效范围
 * <p>翻译键：{@code error.core_cost_out_of_range}</p>
 */
public class CoreCostOutOfRangeError extends ParameterException {

    public CoreCostOutOfRangeError(StateMachine executor, int value, int min, int max) {
        super(executor, "error.core_cost_out_of_range", value, min, max);
    }
}
