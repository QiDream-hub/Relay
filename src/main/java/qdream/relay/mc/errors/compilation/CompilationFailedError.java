package qdream.relay.mc.errors.compilation;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.errors.ExecutionException;

/**
 * 程序解析失败（运行期）
 * <p>
 * 磁盘中的程序在运行期解析失败时抛出。注意这属于执行期错误，
 * 与编译期抛出的 {@link CompilationException} 不是同一套继承体系。
 * </p>
 * <p>翻译键：{@code error.compilation_failed}</p>
 */
public class CompilationFailedError extends ExecutionException {

    public CompilationFailedError(StateMachine executor, String detail) {
        super(executor, "error.compilation_failed", detail);
    }
}
