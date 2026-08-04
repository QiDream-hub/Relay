package qdream.relay.mc.errors;

/**
 * 参数验证异常
 * <p>
 * 用于操作参数验证失败的错误，如参数超出范围、参数格式错误等
 */
public class ParameterException extends ExecutionException {
    public ParameterException(String message) {
        super(message);
    }

    public ParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
