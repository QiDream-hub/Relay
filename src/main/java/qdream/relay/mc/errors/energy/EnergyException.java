package qdream.relay.mc.errors.energy;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.ExecutionException;

/**
 * 能量异常基类
 * <p>
 * 能量系统相关的错误消息类型都继承此类，如能量不足、能量模块缺失等。
 * </p>
 */
public abstract class EnergyException extends ExecutionException {

    protected EnergyException(StateMachine executor, String translationKey, Object... args) {
        super(executor, translationKey, args);
    }

    protected EnergyException(StateMachine executor, Component prefix, String translationKey, Object... args) {
        super(executor, prefix, translationKey, args);
    }
}
