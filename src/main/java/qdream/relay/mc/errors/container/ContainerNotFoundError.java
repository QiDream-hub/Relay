package qdream.relay.mc.errors.container;

import qdream.relay.engine.StateMachine;

/**
 * 容器不存在
 * <p>翻译键：{@code error.container_not_found}</p>
 */
public class ContainerNotFoundError extends ContainerException {

    public ContainerNotFoundError(StateMachine executor) {
        super(executor, "error.container_not_found");
    }
}
