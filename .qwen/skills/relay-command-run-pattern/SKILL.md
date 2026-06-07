---
name: relay-command-run-pattern
description: Relay 模组中实现运行法术磁盘程序的命令模式，包括 ProgramCompiler 编译器、StateMachine 集成、事故处理和结果反馈
source: auto-skill
extracted_at: '2026-06-08T00:00:00.000Z'
---

# Relay 命令运行法术程序模式

## 概述

在 Relay 模组中，实现一个命令来运行法术磁盘中的程序需要以下关键步骤：

1. **编译程序字符串** - 使用 `ProgramCompiler.compile()` 将文本编译为 `List<Executable>`
2. **从法术磁盘读取程序** - 使用 `SpellDiskItem.getProgram()`
3. **创建 StateMachine 实例**
4. **设置事故处理器**
5. **加载程序并执行 tick**
6. **反馈执行结果**

## 程序编译器（ProgramCompiler）

**位置**: `qdream.relay.engine.ProgramCompiler`

将文本格式的程序字符串编译为 `Executable` 列表，支持递归嵌套解析。

### 支持的语法

```
数据字面量：number(1), bool(true), str("hello"), vec(1,2,3), entity(uuid), list(...), null
操作指令：relay:add, relay:pop, ...（直接使用操作 ID）
分隔符：分号 ;
```

### 嵌套列表支持

`parseList()` 方法内部递归调用 `parseInstruction()`，支持任意深度嵌套：

```java
// 示例：嵌套列表
list(
    number(1);
    list(number(2);number(3));  // 嵌套列表
    str("hello")
)
```

### 编译 API

```java
try {
    List<Executable> program = ProgramCompiler.compile(programStr);
} catch (CompilationException e) {
    // 处理编译错误
    source.sendFailure(Component.literal("§c 程序编译失败：" + e.getMessage()));
}
```

### 编译器实现要点

1. **递归下降解析** - 使用 `pos` 指针遍历输入字符串
2. **类型识别** - 通过前缀识别不同类型（`number(`, `bool(`, `list(` 等）
3. **操作查找** - 使用 `OperationRegistry.get(opId)` 获取操作实例
4. **错误处理** - 抛出 `CompilationException` 带明确错误信息

## 法术程序语法（旧版参考）

### JSON 格式（已废弃）

旧版本使用 JSON 格式，现已改用编译器语法：

```
{"type":"relay:number","value":1};{"type":"relay:number","value":1};relay:add
```

## 命令结构

```java
// /relay run <hand|shell> <ops>
LiteralCommandNode<CommandSourceStack> run = Commands.literal("run")
        .then(Commands.argument("target", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    builder.suggest("hand");
                    builder.suggest("shell");
                    return builder.buildFuture();
                })
                .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                        .executes(RelayCommands::runHand)
                )
                .then(Commands.literal("shell")
                        .then(Commands.argument("pos", StringArgumentType.word())
                                .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                                        .executes(RelayCommands::runShell)
                                )
                        )
                )
        )
        .build();
```

## 核心实现模式

### 1. 手中磁盘运行

```java
private static int runHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    CommandSourceStack source = context.getSource();
    var player = source.getPlayerOrException();
    ItemStack stack = player.getMainHandItem();

    // 检查物品类型
    if (!(stack.getItem() instanceof SpellDiskItem)) {
        throw NO_DISK.create();
    }

    // 获取操作数参数
    int ops = IntegerArgumentType.getInteger(context, "ops");

    // 从磁盘读取程序
    List<Executable> program = SpellDiskItem.getProgram(stack);
    if (program.isEmpty()) {
        source.sendSuccess(() -> Component.literal("法术磁盘为空，无法运行"), true);
        return 0;
    }

    // 创建状态机并设置事故处理器
    StateMachine machine = new StateMachine();
    machine.setMishapHandler(reason -> {
        try {
            source.sendFailure(Component.literal("§c 事故：" + reason));
        } catch (Exception e) {
            // 忽略回调中的异常
        }
    });

    // 加载程序并执行
    machine.loadProgram(program);
    machine.tick(ops);

    // 反馈结果
    List<Executable> dataStack = machine.getDataStackSnapshot();
    StringBuilder result = new StringBuilder();
    result.append("运行完成 (剩余 §e").append(machine.getRemainingOps()).append("§r 操作)");
    if (!dataStack.isEmpty()) {
        result.append(", 数据栈：§f").append(dataStackToString(dataStack));
    }

    source.sendSuccess(() -> Component.literal(result.toString()), true);
    return 1;
}
```

### 2. 写入程序到磁盘（使用编译器）

```java
private static int writeSpellToHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    CommandSourceStack source = context.getSource();
    var player = source.getPlayerOrException();
    ItemStack stack = player.getMainHandItem();

    if (!(stack.getItem() instanceof SpellDiskItem)) {
        throw NO_DISK.create();
    }

    String programStr = StringArgumentType.getString(context, "program");
    List<Executable> program;
    try {
        // 使用 ProgramCompiler 编译程序字符串
        program = ProgramCompiler.compile(programStr);
    } catch (CompilationException e) {
        source.sendFailure(Component.literal("§c 程序编译失败：" + e.getMessage()));
        return 0;
    }

    SpellDiskItem.setProgram(stack, program);
    source.sendSuccess(() -> Component.literal("已写入法术程序：§e" + program.size() + "§r 个指令"), true);
    return program.size();
}
```

### 3. 外壳方块磁盘运行

```java
private static int runShell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    CommandSourceStack source = context.getSource();
    String posStr = StringArgumentType.getString(context, "pos");
    int ops = IntegerArgumentType.getInteger(context, "ops");

    // 解析方块坐标
    var pos = parseBlockPos(posStr, source);
    BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

    // 检查方块实体类型
    if (!(blockEntity instanceof ShellContainer shell)) {
        throw INVALID_SLOT.create();
    }

    // 获取法术磁盘
    ItemStack disk = shell.getDiskStack();
    if (disk.isEmpty() || !(disk.getItem() instanceof SpellDiskItem)) {
        throw NO_DISK.create();
    }

    // 后续逻辑与 runHand 相同...
}
```

## 关键组件

### StateMachine 使用

```java
// 创建状态机（默认最大栈大小 1024）
StateMachine machine = new StateMachine();

// 设置事故处理器（必需）
machine.setMishapHandler(reason -> {
    // 处理事故，如发送错误消息
});

// 加载程序（程序列表会被自动反转后压入程序栈）
machine.loadProgram(program);

// 执行 tick（ops 为可用操作数）
machine.tick(ops);

// 获取执行结果
List<Executable> dataStack = machine.getDataStackSnapshot();
int remainingOps = machine.getRemainingOps();
boolean isRunning = machine.isRunning();
```

### 事故处理

事故（Mishap）是 Relay 执行模型中的错误处理机制。触发事故时：
- 清空程序栈和数据栈
- 停止执行
- 调用事故处理器回调

常见事故原因：
- 未知操作
- 数据栈为空时弹出
- 栈超出大小限制
- 操作需要世界交互器但未提供
- 操作数不足

### 结果反馈

```java
// 数据栈转字符串辅助方法
private static String dataStackToString(List<Executable> dataStack) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < dataStack.size(); i++) {
        Executable data = dataStack.get(i);
        if (i > 0) sb.append(", ");

        if (data instanceof StringIota s) {
            sb.append("\"").append(s.asString()).append("\"");
        } else if (data instanceof NumberIota n) {
            sb.append(n.getValue());
        } else if (data instanceof BooleanIota b) {
            sb.append(b.asBoolean());
        } else {
            sb.append(((Operation) data).getId());
        }
    }
    return sb.toString();
}
```

## 使用示例

```bash
# 运行手中磁盘程序，最多 100 个操作
/relay run hand 100

# 运行坐标 (0,64,0) 处外壳中的程序，最多 500 个操作
/relay run shell 0,64,0 500

# 写入简单程序
/relay write hand 'number(1);number(2);relay:add'

# 写入嵌套列表程序
/relay write hand 'list(number(1);list(number(2);number(3));relay:pop)'
```

## 注意事项

1. **操作数限制**: ops 参数范围 1-10000，防止过度执行
2. **事故回调线程安全**: 事故处理器可能在主线程外调用，避免在回调中修改游戏状态
3. **程序反转**: `loadProgram()` 会自动反转程序列表，保证从左到右的执行顺序
4. **数据栈快照**: `getDataStackSnapshot()` 返回栈底到栈顶的顺序
5. **编译器错误处理**: 使用 `CompilationException` 捕获编译错误，提供明确的错误位置信息
6. **嵌套列表**: `list()` 语法支持任意深度嵌套，通过递归解析实现

## 验证步骤

```bash
# 构建验证
./gradlew build

# 测试编译器语法
/relay write hand 'number(5);number(3);relay:add'
/relay run hand 10
# 预期输出：运行完成 (剩余 X 操作), 数据栈：8

# 测试嵌套列表
/relay write hand 'list(number(1);list(number(2);number(3)))'
/relay read hand
# 预期输出：法术程序 (1 个指令): [...]
```
