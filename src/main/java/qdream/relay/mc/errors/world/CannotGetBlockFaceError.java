package qdream.relay.mc.errors.world;

import qdream.relay.engine.StateMachine;

/**
 * 无法获取方块面
 * <p>翻译键：{@code error.cannot_get_block_face}</p>
 */
public class CannotGetBlockFaceError extends WorldInteractionException {

    public CannotGetBlockFaceError(StateMachine executor) {
        super(executor, "error.cannot_get_block_face");
    }
}
