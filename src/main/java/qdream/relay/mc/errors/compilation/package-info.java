/**
 * 编译期错误
 *
 * <p>{@link qdream.relay.mc.errors.compilation.CompilationException} 是编译磁盘程序时
 * 抛出的错误基类，属于普通 {@code RuntimeException}，不会触发状态机事故。</p>
 *
 * <p>{@link qdream.relay.mc.errors.compilation.CompilationFailedError} 是它的运行期对应物：
 * 外壳在运行期解析磁盘程序失败时抛出，因此直接继承
 * {@link qdream.relay.mc.errors.ExecutionException}。</p>
 */
package qdream.relay.mc.errors.compilation;
