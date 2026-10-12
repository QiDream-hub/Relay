package qdream.relay.mc.errors.compilation;

/**
 * NBT 缺少指令 ID
 * <p>翻译键：{@code error.compilation.missing_instruction_id}</p>
 */
public class CompilationMissingInstructionIdError extends CompilationException {

    public CompilationMissingInstructionIdError() {
        super("error.compilation.missing_instruction_id");
    }
}
