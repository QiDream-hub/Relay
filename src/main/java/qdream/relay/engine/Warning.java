package qdream.relay.engine;

/**
 * 引擎警告异常基类
 * <p>
 * 所有引擎执行时的异常都应继承此异常，包括状态机层面的警告和操作执行错误
 * <p>
 * 使用示例:
 * 
 * <pre>{@code
 * // 状态机层面警告
 * throw new Warning("程序已运行完成");
 * 
 * // 操作执行错误
 * throw new ExecutionException("参数类型错误");
 * }</pre>
 */
public class Warning extends RuntimeException {
    /**
     * 构造警告异常
     *
     * @param message 警告消息
     */
    public Warning(StateMachine executor, String message) {
        super(message);
        executor.triggerMishap(message);
    }

    /**
     * 构造警告异常
     *
     * @param message 警告消息
     * @param cause   根本原因
     */
    public Warning(StateMachine executor, String message, Throwable cause) {
        super(message, cause);
        executor.triggerMishap(message);
    }
}
