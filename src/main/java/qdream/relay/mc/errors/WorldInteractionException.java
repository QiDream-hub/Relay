package qdream.relay.mc.errors;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 世界交互异常
 * <p>
 * 用于需要世界交互器的操作错误，如世界交互器缺失、超出范围、世界不存在等
 */
public class WorldInteractionException extends ExecutionException {
    public WorldInteractionException(StateMachine executor, Component message) {
        super(executor, message);
    }

    public WorldInteractionException(StateMachine executor, Component message, Throwable cause) {
        super(executor, message, cause);
    }
}
