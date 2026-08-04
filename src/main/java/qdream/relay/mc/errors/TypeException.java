package qdream.relay.mc.errors;

/**
 * 类型错误异常
 * <p>
 * 用于类型不匹配的错误，如期望数字但得到字符串、类型转换失败等
 */
public class TypeException extends ExecutionException {
    public TypeException(String message) {
        super(message);
    }

    public TypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
