package qdream.relay.mc.errors.entity;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.ExecutionException;

/**
 * 实体引用异常基类
 * <p>
 * 实体相关的错误消息类型都继承此类，如实体引用无效、实体类型不匹配、实体不存在等。
 * </p>
 */
public abstract class EntityException extends ExecutionException {

    protected EntityException(StateMachine executor, String translationKey, Object... args) {
        super(executor, translationKey, args);
    }

    protected EntityException(StateMachine executor, Component prefix, String translationKey, Object... args) {
        super(executor, prefix, translationKey, args);
    }
}
