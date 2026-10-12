package qdream.relay.mc.errors.stack;

import qdream.relay.engine.StateMachine;

/**
 * 无法获取目标元素
 * <p>翻译键：{@code error.element_not_gettable}</p>
 */
public class ElementNotGettableError extends StackException {

    public ElementNotGettableError(StateMachine executor) {
        super(executor, "error.element_not_gettable");
    }
}
