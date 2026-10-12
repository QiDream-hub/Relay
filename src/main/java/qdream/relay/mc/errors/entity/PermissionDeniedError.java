package qdream.relay.mc.errors.entity;

import qdream.relay.engine.StateMachine;

/**
 * 无权执行此操作
 * <p>翻译键：{@code error.permission_denied}</p>
 */
public class PermissionDeniedError extends EntityException {

    public PermissionDeniedError(StateMachine executor) {
        super(executor, "error.permission_denied");
    }
}
