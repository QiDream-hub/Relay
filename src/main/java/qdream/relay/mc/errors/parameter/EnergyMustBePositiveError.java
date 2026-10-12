package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 能量值必须大于 0
 * <p>翻译键：{@code error.energy_must_be_positive}</p>
 */
public class EnergyMustBePositiveError extends ParameterException {

    public EnergyMustBePositiveError(StateMachine executor) {
        super(executor, "error.energy_must_be_positive");
    }
}
