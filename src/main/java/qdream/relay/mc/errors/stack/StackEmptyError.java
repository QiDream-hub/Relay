package qdream.relay.mc.errors.stack;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 数据栈为空
 * <p>翻译键：{@code error.stack_empty}</p>
 */
public class StackEmptyError extends StackException {

    public StackEmptyError(StateMachine executor) {
        super(executor, "error.stack_empty");
    }

    public StackEmptyError(StateMachine executor, Component prefix) {
        super(executor, prefix, "error.stack_empty");
    }
}
