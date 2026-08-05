package qdream.relay.mc.errors;

import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Warning;

/**
 * 法术执行异常基类
 * <p>
 * 所有操作中的错误都应抛出此异常或其子类，而不是直接调用 triggerMishap()
 * <p>
 * 使用示例:
 * 
 * <pre>{@code
 * public void execute(StateMachine executor) {
 *     if (errorCondition) {
 *         throw new ExecutionException("错误描述");
 *     }
 *     // 正常逻辑
 * }
 * }</pre>
 */
public class ExecutionException extends Warning {
    /**
     * 构造执行异常
     *
     * @param message 错误消息
     */
    public ExecutionException(StateMachine executor, String message) {
        super(executor,message);
    }

    /**
     * 构造执行异常
     *
     * @param message 错误消息
     * @param cause   根本原因
     */
    public ExecutionException(StateMachine executor, String message, Throwable cause) {
        super(executor,message, cause);
    }
}
