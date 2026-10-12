package qdream.relay.mc.errors.type;

import qdream.relay.engine.StateMachine;

/**
 * 方块实体引用无效
 * <p>翻译键：{@code error.block_entity_reference_invalid}</p>
 */
public class BlockEntityReferenceInvalidError extends TypeException {

    public BlockEntityReferenceInvalidError(StateMachine executor) {
        super(executor, "error.block_entity_reference_invalid");
    }
}
