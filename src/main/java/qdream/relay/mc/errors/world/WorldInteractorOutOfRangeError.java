package qdream.relay.mc.errors.world;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 超出世界交互器范围
 * <p>翻译键：{@code error.world_interactor_out_of_range}</p>
 */
public class WorldInteractorOutOfRangeError extends WorldInteractionException {

    public WorldInteractorOutOfRangeError(StateMachine executor, Component operationName, double distance, double range) {
        super(executor, "error.world_interactor_out_of_range", operationName, distance, range);
    }
}
