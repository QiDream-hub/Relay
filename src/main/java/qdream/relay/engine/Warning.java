package qdream.relay.engine;

import java.util.function.Function;

/**
 * 引擎警告异常基类
 * <p>
 * 所有引擎执行时的异常都应继承此异常，包括状态机层面的警告和操作执行错误
 */
public class Warning extends RuntimeException {
    private final Object info;

    /**
     * 构造警告异常（使用 Component）
     *
     * @param executor 状态机
     * @param info     错误信息（Component）
     */
    public Warning(StateMachine executor, Object info, Function<Object, String> fn) {
        super(fn.apply(info));
        this.info = info;
        executor.triggerMishap(this);
    }

    /**
     * 构造警告异常（使用 Component 和根本原因）
     *
     * @param executor 状态机
     * @param info     错误信息
     * @param cause    根本原因
     */
    public Warning(StateMachine executor, Object info, Function<Object, String> fn, Throwable cause) {
        super(fn.apply(info), cause);
        this.info = info;
        executor.triggerMishap(this);
    }

    /**
     * 获取错误信息
     *
     * @return 原始信息对象
     */
    public Object getInfo() {
        return info;
    }
}
