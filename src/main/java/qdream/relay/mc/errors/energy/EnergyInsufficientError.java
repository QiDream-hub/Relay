package qdream.relay.mc.errors.energy;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 能量不足
 * <p>翻译键：{@code error.energy_insufficient}</p>
 */
public class EnergyInsufficientError extends EnergyException {

    public EnergyInsufficientError(StateMachine executor, double required) {
        super(executor, "error.energy_insufficient", required);
    }

    public EnergyInsufficientError(StateMachine executor, Component prefix, double required) {
        super(executor, prefix, "error.energy_insufficient", required);
    }
}
