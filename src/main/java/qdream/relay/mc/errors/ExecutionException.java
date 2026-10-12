package qdream.relay.mc.errors;

import java.util.function.Function;

import net.minecraft.network.chat.Component;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Warning;

/**
 * 法术执行异常基类
 * <p>
 * 所有操作中的错误都应抛出此异常或其子类，而不是直接调用 triggerMishap()。
 * 每条错误消息对应一个具体的错误类型（子类），消息所需的参数通过构造函数传入。
 * </p>
 * <p>
 * 具体错误按来源放在本包的各子包中（{@code stack}、{@code type}、{@code parameter}、
 * {@code entity}、{@code container}、{@code energy}、{@code world}、{@code communication}），
 * 每个子包以一个抽象基类为中心。
 * </p>
 */
public class ExecutionException extends Warning {

    /** 统一错误消息颜色 */
    public static final int ERROR_COLOR = 0x55FFFF;

    /** 将 Warning 保存的信息对象解析为异常消息 */
    private static final Function<Object, String> MESSAGE_RESOLVER = info -> {
        if (info instanceof Component component) {
            return component.getString();
        }
        return "未知错误";
    };

    /**
     * 构造执行异常（使用已构建的消息组件）
     *
     * @param executor 状态机
     * @param info     错误信息（Component）
     */
    public ExecutionException(StateMachine executor, Component info) {
        super(executor, info, MESSAGE_RESOLVER);
    }

    /**
     * 构造执行异常（使用已构建的消息组件和根本原因）
     *
     * @param executor 状态机
     * @param info     错误信息（Component）
     * @param cause    根本原因
     */
    public ExecutionException(StateMachine executor, Component info, Throwable cause) {
        super(executor, info, MESSAGE_RESOLVER, cause);
    }

    /**
     * 构造执行异常（使用翻译键和消息参数）
     *
     * @param executor       状态机
     * @param translationKey 翻译键
     * @param args           消息参数（按翻译键中占位符的顺序）
     */
    protected ExecutionException(StateMachine executor, String translationKey, Object... args) {
        this(executor, buildMessage(translationKey, args));
    }

    /**
     * 构造执行异常（在消息前附加前缀组件）
     *
     * @param executor       状态机
     * @param prefix         前缀组件（例如操作名称）
     * @param translationKey 翻译键
     * @param args           消息参数（按翻译键中占位符的顺序）
     */
    protected ExecutionException(StateMachine executor, Component prefix, String translationKey, Object... args) {
        this(executor, prefix.copy().append(buildMessage(translationKey, args)));
    }

    /**
     * 根据翻译键和参数构建统一颜色的错误消息组件
     *
     * @param translationKey 翻译键
     * @param args           消息参数
     * @return 错误消息组件
     */
    private static Component buildMessage(String translationKey, Object... args) {
        return Component.translatable(translationKey, args).withColor(ERROR_COLOR);
    }
}
