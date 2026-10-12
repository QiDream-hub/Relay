package qdream.relay.mc.errors.compilation;

import net.minecraft.network.chat.Component;

/**
 * JSON 解析失败
 * <p>翻译键：{@code error.compilation.json_syntax}</p>
 */
public class CompilationJsonSyntaxError extends CompilationException {

    public CompilationJsonSyntaxError(String detail, Throwable cause) {
        super(Component.translatable("error.compilation.json_syntax", detail), cause);
    }
}
