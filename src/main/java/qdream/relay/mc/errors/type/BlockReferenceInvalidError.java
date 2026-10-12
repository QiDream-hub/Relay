package qdream.relay.mc.errors.type;

import qdream.relay.engine.StateMachine;

/**
 * 方块引用无效
 * <p>翻译键：{@code error.block_reference_invalid}</p>
 */
public class BlockReferenceInvalidError extends TypeException {

    public BlockReferenceInvalidError(StateMachine executor) {
        super(executor, "error.block_reference_invalid");
    }
}
