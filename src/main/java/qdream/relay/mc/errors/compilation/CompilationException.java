package qdream.relay.mc.errors.compilation;

import net.minecraft.network.chat.Component;

/**
 * 法术编译异常基类
 * <p>
 * 所有程序编译时的错误都应抛出此异常的子类，支持多语言错误提示。
 * 每条编译错误消息对应一个具体类型，消息参数通过构造函数传入。
 * </p>
 */
public abstract class CompilationException extends RuntimeException {

    private final Component info;

    /**
     * 构造编译异常（使用已构建的消息组件）
     *
     * @param info 错误信息（Component）
     */
    protected CompilationException(Component info) {
        super(info.getString());
        this.info = info;
    }

    /**
     * 构造编译异常（使用已构建的消息组件和根本原因）
     *
     * @param info  错误信息
     * @param cause 根本原因
     */
    protected CompilationException(Component info, Throwable cause) {
        super(info.getString(), cause);
        this.info = info;
    }

    /**
     * 构造编译异常（使用翻译键和消息参数）
     *
     * @param translationKey 翻译键
     * @param args           消息参数（按翻译键中占位符的顺序）
     */
    protected CompilationException(String translationKey, Object... args) {
        this(Component.translatable(translationKey, args));
    }

    /**
     * 获取错误信息
     *
     * @return Component 形式的错误消息
     */
    public Component getComponent() {
        return info;
    }
}
