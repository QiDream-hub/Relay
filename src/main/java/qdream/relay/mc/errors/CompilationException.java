package qdream.relay.mc.errors;

import net.minecraft.network.chat.Component;

/**
 * 法术编译异常
 * <p>
 * 所有程序编译时的错误都应抛出此异常或其子类，支持多语言错误提示
 */
public class CompilationException extends RuntimeException {
    private final Component info;

    /**
     * 构造编译异常（使用 Component）
     *
     * @param info 错误信息（Component）
     */
    public CompilationException(Component info) {
        super(info.getString());
        this.info = info;
    }

    /**
     * 构造编译异常（使用 Component 和根本原因）
     *
     * @param info  错误信息
     * @param cause 根本原因
     */
    public CompilationException(Component info, Throwable cause) {
        super(info.getString(), cause);
        this.info = info;
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
