package qdream.relay.mc.errors;

/**
 * 栈操作异常
 * <p>
 * 用于数据栈或程序栈相关的错误，如栈为空、栈溢出、索引越界等
 */
public class StackException extends ExecutionException {
    public StackException(String message) {
        super(message);
    }

    public StackException(String message, Throwable cause) {
        super(message, cause);
    }
}
