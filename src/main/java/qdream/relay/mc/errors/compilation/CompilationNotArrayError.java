package qdream.relay.mc.errors.compilation;

/**
 * JSON 程序必须是数组格式
 * <p>翻译键：{@code error.compilation.not_array}</p>
 */
public class CompilationNotArrayError extends CompilationException {

    public CompilationNotArrayError() {
        super("error.compilation.not_array");
    }
}
