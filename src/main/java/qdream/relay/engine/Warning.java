package qdream.relay.engine;

import java.util.function.Function;

/**
 * 引擎警告异常基类
 * <p>
 * 所有引擎执行时的异常都应继承此异常，包括状态机层面的警告和操作执行错误。
 * </p>
 * <p>
 * <b>构造即上报：</b>构造函数会直接调用 {@link StateMachine#triggerMishap(Warning)}，
 * 因此 {@code new XxxError(...)} 本身就会清空状态机双栈并通知 {@code MishapHandler}，
 * 调用方不需要（也不应该）再显式触发一次。同一个实例最多只会被上报一次，
 * 重复触发是空操作。
 * </p>
 */
public class Warning extends RuntimeException {
    private final Object info;

    /** 是否已经上报过，保证同一个警告实例只会上报一次 */
    private boolean reported;

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
     * 标记该警告已被上报
     * <p>
     * 由 {@link StateMachine#triggerMishap(Warning)} 调用，用于去重。
     * </p>
     *
     * @return 本次调用为首次上报时返回 {@code true}，已经上报过时返回 {@code false}
     */
    boolean markReported() {
        if (reported) {
            return false;
        }
        reported = true;
        return true;
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
