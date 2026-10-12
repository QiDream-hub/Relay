package qdream.relay.mc.errors.container;

import qdream.relay.engine.StateMachine;

/**
 * 目标不是容器
 * <p>翻译键：{@code error.not_a_container}</p>
 */
public class NotAContainerError extends ContainerException {

    public NotAContainerError(StateMachine executor) {
        super(executor, "error.not_a_container");
    }
}
