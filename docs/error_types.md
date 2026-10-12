# 错误类型组织与约定

## 概述

所有错误类型位于 `qdream.relay.mc.errors`。这个包里其实有**两套互不相干的继承体系**：

| 体系 | 根类型 | 父类 | 是否会触发状态机事故 |
|------|--------|------|----------------------|
| 运行期错误 | `ExecutionException` | `engine.Warning`（`RuntimeException`） | 会 |
| 编译期错误 | `compilation.CompilationException` | `RuntimeException` | 不会 |

运行期错误按**错误来源**分子包，每个子包以一个抽象基类为中心：

```
mc/errors/
├── ExecutionException.java      运行期错误根类型
├── compilation/                 CompilationException
│   └── CompilationFailedError   运行期解析失败，直接继承 ExecutionException
├── stack/                       StackException      数据栈 / 程序栈
├── type/                        TypeException       运行时类型不符
├── parameter/                   ParameterException   参数取值 / 格式 / 范围非法
├── entity/                      EntityException      实体相关
├── container/                   ContainerException   容器 / 物品栏
├── energy/                      EnergyException      能量不足
├── world/                       WorldInteractionException  世界交互器 / 世界上下文
└── communication/               （无基类）通信频道
```

具体错误类放在对应基类所在的子包中，例如
`stack/StackEmptyError extends StackException`。

## 约定

1. **一个错误消息一个类。** 消息参数通过构造函数以强类型传入，
   不要用 `triggerMishap("error.xxx", args...)` 这种字符串形式，否则参数写错只能在运行时发现。
2. **翻译键与类一一对应，且不重复。** 键名统一使用 `error.` 前缀，子包不改变键名
   （`stack/StackEmptyError` 的键仍然是 `error.stack_empty`，不是 `error.stack.stack_empty`）。
3. **抽象基类不携带翻译键**，消息文本一律由具体子类提供。
4. **构造即上报事故。** `Warning` 的构造函数会调用 `StateMachine.triggerMishap(this)`，
   因此 `new SomeError(executor, ...)` 本身就会清空状态机双栈并通知 `MishapHandler`。
   - 处在操作的执行路径上时直接 `throw` 即可，异常会一路上抛到 `ShellTickHandler` 收口，
     **不要**再额外显式调用一次 `triggerMishap`；
   - 只有不在执行路径上、需要主动中止时才显式调用 `stateMachine.triggerMishap(...)`
     （例如 `ShellTickHandler` 的 tick 循环里发现能量不足），此时构造函数触发的那一次会被去重，
     不会重复上报给玩家；
   - 如果某处需要 `catch` 住它并继续（例如检查失败时压入一个默认值），
     事故在 `catch` 执行之前就已经触发过了，保持这个语义不要改动。

## 新增一个错误类型

1. 判断它属于哪个来源，放进对应子包；确实不属于任何一类时放进 `communication/` 这类小包，
   或直接继承 `ExecutionException`。
2. 新建类，继承该子包的基类，构造函数只负责传翻译键和参数。
3. 在 `src/main/resources/assets/relay/lang/` 的 `zh_cn.json` 与 `en_us.json` 中**同时**补上翻译键。
4. 运行校验：

```bash
./gradlew checkErrorLangs
```

## 校验

`./gradlew checkErrorLangs`（已挂到 `check` / `build` 上，由 `qdream.relay.tools.CheckErrorLangs` 实现）
会检查：

- 每个叶子错误类恰好使用一个 `error.*` 键，抽象基类不携带键；
- 翻译键没有被两个类共用；
- 用到的键在每份语言文件里都存在；
- 语言文件里的 `error.*` 键都能在源码中找到引用（防止遗留死键）；
- 两份语言文件的 `error.*` 键集合一致；
- 同一个键在两份语言文件里的格式占位符一致（作为警告输出）。

有任何一条不满足即以非 0 退出码结束构建。
