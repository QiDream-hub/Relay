package qdream.relay.mc.errors.compilation;

/**
 * 未知的指令
 * <p>翻译键：{@code error.compilation.unknown_instruction}</p>
 */
public class CompilationUnknownInstructionError extends CompilationException {

    public CompilationUnknownInstructionError(String instructionId) {
        super("error.compilation.unknown_instruction", instructionId);
    }
}
