package qdream.relay.mc.errors.entity;

import qdream.relay.engine.StateMachine;

/**
 * 无法获取所属者
 * <p>翻译键：{@code error.owner_not_found}</p>
 */
public class OwnerNotFoundError extends EntityException {

    public OwnerNotFoundError(StateMachine executor) {
        super(executor, "error.owner_not_found");
    }
}
