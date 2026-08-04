package qdream.relay.mc.errors;

/**
 * 实体引用异常
 * <p>
 * 用于实体相关的错误，如实体引用无效、实体类型不匹配、实体不存在等
 */
public class EntityException extends ExecutionException {
    public EntityException(String message) {
        super(message);
    }

    public EntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
