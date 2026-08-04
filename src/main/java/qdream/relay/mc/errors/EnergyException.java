package qdream.relay.mc.errors;

/**
 * 能量不足异常
 * <p>
 * 用于能量系统相关的错误，如能量不足、能量模块缺失等
 */
public class EnergyException extends ExecutionException {
    public EnergyException(String message) {
        super(message);
    }

    public EnergyException(String message, Throwable cause) {
        super(message, cause);
    }
}
