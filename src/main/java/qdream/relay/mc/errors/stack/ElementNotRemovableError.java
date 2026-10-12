package qdream.relay.mc.errors.stack;

import qdream.relay.engine.StateMachine;

/**
 * 无法移除目标元素
 * <p>翻译键：{@code error.element_not_removable}</p>
 */
public class ElementNotRemovableError extends StackException {

    public ElementNotRemovableError(StateMachine executor) {
        super(executor, "error.element_not_removable");
    }
}
