package qdream.relay.mc.errors.world;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 无法获取世界
 * <p>翻译键：{@code error.world_not_available}</p>
 */
public class WorldNotAvailableError extends WorldInteractionException {

    public WorldNotAvailableError(StateMachine executor) {
        super(executor, "error.world_not_available");
    }

    public WorldNotAvailableError(StateMachine executor, Component prefix) {
        super(executor, prefix, "error.world_not_available");
    }
}
