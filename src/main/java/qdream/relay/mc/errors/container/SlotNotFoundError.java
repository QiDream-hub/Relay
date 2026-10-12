package qdream.relay.mc.errors.container;

import qdream.relay.engine.StateMachine;

/**
 * 插槽不存在
 * <p>翻译键：{@code error.slot_not_found}</p>
 */
public class SlotNotFoundError extends ContainerException {

    public SlotNotFoundError(StateMachine executor) {
        super(executor, "error.slot_not_found");
    }
}
