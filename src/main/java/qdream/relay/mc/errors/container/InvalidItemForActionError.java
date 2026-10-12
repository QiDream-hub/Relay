package qdream.relay.mc.errors.container;

import qdream.relay.engine.StateMachine;

/**
 * 错误的物品
 * <p>翻译键：{@code error.invalid_item_for_action}</p>
 */
public class InvalidItemForActionError extends ContainerException {

    public InvalidItemForActionError(StateMachine executor) {
        super(executor, "error.invalid_item_for_action");
    }
}
