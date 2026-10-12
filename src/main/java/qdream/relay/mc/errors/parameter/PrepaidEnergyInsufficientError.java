package qdream.relay.mc.errors.parameter;

import qdream.relay.engine.StateMachine;

/**
 * 预付能量不足
 * <p>翻译键：{@code error.prepaid_energy_insufficient}</p>
 */
public class PrepaidEnergyInsufficientError extends ParameterException {

    public PrepaidEnergyInsufficientError(StateMachine executor, double energy, double minimum) {
        super(executor, "error.prepaid_energy_insufficient", energy, minimum);
    }
}
