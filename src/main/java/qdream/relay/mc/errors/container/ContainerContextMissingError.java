package qdream.relay.mc.errors.container;

import qdream.relay.engine.StateMachine;

/**
 * 无法获取容器上下文
 * <p>翻译键：{@code error.container_context_missing}</p>
 */
public class ContainerContextMissingError extends ContainerException {

    public ContainerContextMissingError(StateMachine executor) {
        super(executor, "error.container_context_missing");
    }
}
