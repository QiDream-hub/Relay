package qdream.relay.mc.errors.stack;

import qdream.relay.engine.StateMachine;

/**
 * 无法设置目标元素
 * <p>翻译键：{@code error.element_not_settable}</p>
 */
public class ElementNotSettableError extends StackException {

    public ElementNotSettableError(StateMachine executor) {
        super(executor, "error.element_not_settable");
    }
}
