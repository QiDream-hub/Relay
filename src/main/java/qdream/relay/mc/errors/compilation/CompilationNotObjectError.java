package qdream.relay.mc.errors.compilation;

/**
 * 程序元素必须是 JSON 对象
 * <p>翻译键：{@code error.compilation.not_object}</p>
 */
public class CompilationNotObjectError extends CompilationException {

    public CompilationNotObjectError(int index, String data) {
        super("error.compilation.not_object", index, data);
    }
}
