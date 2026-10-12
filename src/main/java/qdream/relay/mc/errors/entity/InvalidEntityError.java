package qdream.relay.mc.errors.entity;

import qdream.relay.engine.StateMachine;

/**
 * 目标实体不可用
 * <p>
 * 用于实体引用能解析、但实体已被移除或不是操作所需类型的情况
 * （例如目标不是物品实体、物品堆为空）。若引用本身无法解析，
 * 应使用 {@link EntityReferenceInvalidError}。
 * </p>
 * <p>翻译键：{@code error.invalid_entity}</p>
 */
public class InvalidEntityError extends EntityException {

    public InvalidEntityError(StateMachine executor) {
        super(executor, "error.invalid_entity");
    }
}
