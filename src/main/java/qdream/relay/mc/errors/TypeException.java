package qdream.relay.mc.errors;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 类型错误异常
 * <p>
 * 用于类型不匹配的错误，如期望数字但得到字符串、类型转换失败等
 */
public class TypeException extends ExecutionException {
    public TypeException(StateMachine executor, Component message) {
        super(executor, message);
    }

    public TypeException(StateMachine executor, Component message, Throwable cause) {
        super(executor, message, cause);
    }
}
