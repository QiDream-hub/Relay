package qdream.relay.mc.errors;

/**
 * 世界交互异常
 * <p>
 * 用于需要世界交互器的操作错误，如世界交互器缺失、超出范围、世界不存在等
 */
public class WorldInteractionException extends ExecutionException {
    public WorldInteractionException(String message) {
        super(message);
    }

    public WorldInteractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
