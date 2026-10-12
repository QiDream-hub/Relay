/**
 * 法术错误类型
 *
 * <p>这里包含两套互不相干的继承体系：</p>
 * <ul>
 * <li><b>运行期错误</b>：以 {@link qdream.relay.mc.errors.ExecutionException} 为根，
 * 继承自 {@code Warning}，<b>构造即触发事故</b>（清空状态机双栈并通知 {@code MishapHandler}）。
 * 按错误来源细分为 {@code stack}、{@code type}、{@code parameter}、{@code entity}、
 * {@code container}、{@code energy}、{@code world}、{@code communication} 子包。</li>
 * <li><b>编译期错误</b>：以 {@link qdream.relay.mc.errors.compilation.CompilationException}
 * 为根，是普通的 {@code RuntimeException}，不会触碰状态机。</li>
 * </ul>
 *
 * <p>约定：</p>
 * <ul>
 * <li>一个错误消息对应一个具体类，消息参数通过构造函数强类型传入；</li>
 * <li>翻译键与具体类一一对应且不重复，统一使用 {@code error.} 前缀，子包不改变键名；</li>
 * <li>可用 {@code ./gradlew checkErrorLangs} 校验上述一致性。</li>
 * </ul>
 */
package qdream.relay.mc.errors;
