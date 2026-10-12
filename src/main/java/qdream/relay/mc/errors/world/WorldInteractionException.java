package qdream.relay.mc.errors.world;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.ExecutionException;

/**
 * 世界交互异常基类
 * <p>
 * 需要世界交互器的操作错误消息类型都继承此类，如世界交互器缺失、超出范围、世界不存在等。
 * </p>
 */
public abstract class WorldInteractionException extends ExecutionException {

    protected WorldInteractionException(StateMachine executor, String translationKey, Object... args) {
        super(executor, translationKey, args);
    }

    protected WorldInteractionException(StateMachine executor, Component prefix, String translationKey, Object... args) {
        super(executor, prefix, translationKey, args);
    }
}
