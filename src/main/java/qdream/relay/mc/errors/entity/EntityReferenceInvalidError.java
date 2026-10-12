package qdream.relay.mc.errors.entity;

import qdream.relay.engine.StateMachine;

/**
 * 实体引用无效
 * <p>翻译键：{@code error.entity_reference_invalid}</p>
 */
public class EntityReferenceInvalidError extends EntityException {

    public EntityReferenceInvalidError(StateMachine executor) {
        super(executor, "error.entity_reference_invalid");
    }
}
