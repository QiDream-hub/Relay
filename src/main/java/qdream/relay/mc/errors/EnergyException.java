package qdream.relay.mc.errors;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 能量不足异常
 * <p>
 * 用于能量系统相关的错误，如能量不足、能量模块缺失等
 */
public class EnergyException extends ExecutionException {
    public EnergyException(StateMachine executor, Component message) {
        super(executor, message);
    }

    public EnergyException(StateMachine executor, Component message, Throwable cause) {
        super(executor, message, cause);
    }
}
