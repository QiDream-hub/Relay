package qdream.relay.mc.errors.type;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.ExecutionException;

/**
 * 类型错误异常基类
 * <p>
 * 类型不匹配或引用类型无效相关的错误消息类型都继承此类。
 * </p>
 */
public abstract class TypeException extends ExecutionException {

    protected TypeException(StateMachine executor, String translationKey, Object... args) {
        super(executor, translationKey, args);
    }

    protected TypeException(StateMachine executor, Component prefix, String translationKey, Object... args) {
        super(executor, prefix, translationKey, args);
    }
}
