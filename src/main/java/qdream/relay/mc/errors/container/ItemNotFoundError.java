package qdream.relay.mc.errors.container;

import qdream.relay.engine.StateMachine;

/**
 * 物品不存在
 * <p>翻译键：{@code error.item_not_found}</p>
 */
public class ItemNotFoundError extends ContainerException {

    public ItemNotFoundError(StateMachine executor) {
        super(executor, "error.item_not_found");
    }
}
