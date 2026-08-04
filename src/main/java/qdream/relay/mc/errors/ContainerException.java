package qdream.relay.mc.errors;

/**
 * 容器/物品栏异常
 * <p>
 * 用于容器相关的错误，如容器不存在、不是容器、物品不存在等
 */
public class ContainerException extends ExecutionException {
    public ContainerException(String message) {
        super(message);
    }

    public ContainerException(String message, Throwable cause) {
        super(message, cause);
    }
}
