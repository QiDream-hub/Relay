package qdream.relay.mc.errors;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 实体引用异常
 * <p>
 * 用于实体相关的错误，如实体引用无效、实体类型不匹配、实体不存在等
 */
public class EntityException extends ExecutionException {
    public EntityException(StateMachine executor, Component message) {
        super(executor, message);
    }

    public EntityException(StateMachine executor, Component message, Throwable cause) {
        super(executor, message, cause);
    }
}
