package qdream.relay.mc.errors;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;

/**
 * 容器/物品栏异常
 * <p>
 * 用于容器相关的错误，如容器不存在、不是容器、物品不存在等
 */
public class ContainerException extends ExecutionException {
    public ContainerException(StateMachine executor, Component message) {
        super(executor, message);
    }

    public ContainerException(StateMachine executor, Component message, Throwable cause) {
        super(executor, message, cause);
    }
}
