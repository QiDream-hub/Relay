package qdream.relay.mc.errors;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Warning;

/**
 * 法术执行异常基类
 * <p>
 * 所有操作中的错误都应抛出此异常或其子类，而不是直接调用 triggerMishap()
 */
public class ExecutionException extends Warning {
    /**
     * 构造执行异常（使用 Component）
     *
     * @param executor 状态机
     * @param info     错误信息（Component）
     */
    public ExecutionException(StateMachine executor, Component info) {
        super(executor, info, r -> {
            if (r instanceof Component component) {
                return component.getString();
            }
            return "未知错误";
        });
    }

    /**
     * 构造执行异常（使用 Component 和根本原因）
     *
     * @param executor 状态机
     * @param info     错误信息
     * @param cause    根本原因
     */
    public ExecutionException(StateMachine executor, Component info, Throwable cause) {
        super(executor, info, r -> {
            if (r instanceof Component component) {
                return component.getString();
            }
            return "未知错误";
        }, cause);
    }
}
