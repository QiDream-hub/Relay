package qdream.relay.mc.errors.compilation;

/**
 * NBT 缺少指令数据
 * <p>翻译键：{@code error.compilation.missing_instruction_data}</p>
 */
public class CompilationMissingInstructionDataError extends CompilationException {

    public CompilationMissingInstructionDataError() {
        super("error.compilation.missing_instruction_data");
    }
}
