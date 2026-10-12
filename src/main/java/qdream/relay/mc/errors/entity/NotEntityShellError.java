package qdream.relay.mc.errors.entity;

import qdream.relay.engine.StateMachine;

/**
 * 目标实体不是可运行实体
 * <p>翻译键：{@code error.not_entity_shell}</p>
 */
public class NotEntityShellError extends EntityException {

    public NotEntityShellError(StateMachine executor) {
        super(executor, "error.not_entity_shell");
    }
}
