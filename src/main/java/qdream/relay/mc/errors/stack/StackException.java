package qdream.relay.mc.errors.stack;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.ExecutionException;

/**
 * 栈操作异常基类
 * <p>
 * 数据栈或程序栈相关的错误消息类型都继承此类，如栈为空、栈不足、索引越界等。
 * </p>
 */
public abstract class StackException extends ExecutionException {

    protected StackException(StateMachine executor, String translationKey, Object... args) {
        super(executor, translationKey, args);
    }

    protected StackException(StateMachine executor, Component prefix, String translationKey, Object... args) {
        super(executor, prefix, translationKey, args);
    }
}
