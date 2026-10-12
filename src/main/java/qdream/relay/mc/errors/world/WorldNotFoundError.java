package qdream.relay.mc.errors.world;

import qdream.relay.engine.StateMachine;

/**
 * 世界不存在
 * <p>翻译键：{@code error.world_not_found}</p>
 */
public class WorldNotFoundError extends WorldInteractionException {

    public WorldNotFoundError(StateMachine executor, String worldId) {
        super(executor, "error.world_not_found", worldId);
    }
}
