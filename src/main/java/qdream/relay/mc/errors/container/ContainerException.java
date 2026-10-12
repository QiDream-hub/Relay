package qdream.relay.mc.errors.container;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.ExecutionException;

/**
 * 容器/物品栏异常基类
 * <p>
 * 容器相关的错误消息类型都继承此类，如容器不存在、不是容器、物品不存在等。
 * </p>
 */
public abstract class ContainerException extends ExecutionException {

    protected ContainerException(StateMachine executor, String translationKey, Object... args) {
        super(executor, translationKey, args);
    }

    protected ContainerException(StateMachine executor, Component prefix, String translationKey, Object... args) {
        super(executor, prefix, translationKey, args);
    }
}
