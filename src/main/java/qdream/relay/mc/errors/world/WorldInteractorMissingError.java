package qdream.relay.mc.errors.world;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 缺少世界交互器
 * <p>翻译键：{@code error.world_interactor_missing}</p>
 */
public class WorldInteractorMissingError extends WorldInteractionException {

    public WorldInteractorMissingError(StateMachine executor, Component operationName) {
        super(executor, "error.world_interactor_missing", operationName);
    }
}
