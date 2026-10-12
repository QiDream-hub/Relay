package qdream.relay.mc.errors.compilation;

/**
 * 指令不是 Operation 类型
 * <p>翻译键：{@code error.compilation.not_operation}</p>
 */
public class CompilationNotOperationError extends CompilationException {

    public CompilationNotOperationError(String instruction) {
        super("error.compilation.not_operation", instruction);
    }
}
