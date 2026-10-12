package qdream.relay.mc.errors.container;

import qdream.relay.engine.StateMachine;

/**
 * 无效的容器
 * <p>翻译键：{@code error.invalid_container}</p>
 */
public class InvalidContainerError extends ContainerException {

    public InvalidContainerError(StateMachine executor) {
        super(executor, "error.invalid_container");
    }
}
