package qdream.relay.mc.errors.parameter;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.ExecutionException;

/**
 * 参数验证异常基类
 * <p>
 * 操作参数验证失败的错误消息类型都继承此类，如参数超出范围、参数格式错误等。
 * </p>
 */
public abstract class ParameterException extends ExecutionException {

    protected ParameterException(StateMachine executor, String translationKey, Object... args) {
        super(executor, translationKey, args);
    }

    protected ParameterException(StateMachine executor, Component prefix, String translationKey, Object... args) {
        super(executor, prefix, translationKey, args);
    }
}
