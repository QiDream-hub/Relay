---
name: relay-command-run-pattern
description: Relay 模组中实现运行法术磁盘程序的命令模式，包括 StateMachine 集成、事故处理和结果反馈
source: auto-skill
extracted_at: '2026-06-06T05:51:38.373Z'
---

# Relay 命令运行法术程序模式

## 概述

在 Relay 模组中，实现一个命令来运行法术磁盘中的程序需要以下关键步骤：

1. 从法术磁盘读取程序
2. 创建 StateMachine 实例
3. 设置事故处理器
4. 加载程序并执行 tick
5. 反馈执行结果

## 法术程序语法

### JSON 格式（推荐）

数据使用 JSON 格式，操作使用完全限定名：

```
{"type":"relay:number","value":1};{"type":"relay:number","value":1};relay:add
```

- **数据**：`{"type":"relay:number","value":1}`（JSON 对象）
- **操作**：`relay:add`, `relay:pop`（完全限定名）
- **分隔符**：分号 `;`

### 兼容旧语法

```
1;1;add  // 数字被解析为字符串，不推荐
```

### 类型注册

在 `Relay.onInitialize()` 中初始化类型注册表：

```java
IotaTypeRegistry.init();
```

支持的数据类型：
- `relay:number` - 数字（整数或浮点数）
- `relay:boolean` - 布尔值
- `relay:string` - 字符串
- `relay:vector` - 三维向量
- `relay:entity` - 实体 UUID
- `relay:list` - 列表
- `relay:null` - 空值

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

### 程序解析

```java
private static List<IExecutable> parseProgram(String programStr) {
    List<IExecutable> program = new ArrayList<>();
    String[] instructions = programStr.split(";");

    for (String instr : instructions) {
        instr = instr.trim();
        if (instr.isEmpty()) continue;

        if (instr.startsWith("{")) {
            // JSON 格式的数据
            try {
                JsonParser parser = new JsonParser();
                JsonElement jsonElem = parser.parse(instr);
                IExecutable iota = IotaTypeRegistry.fromJson(jsonElem.getAsJsonObject());
                program.add(iota);
            } catch (Exception e) {
                throw new RuntimeException("解析 JSON 失败：" + instr, e);
            }
        } else if (instr.startsWith("relay:")) {
            // 完全限定名操作
            program.add(McIota.ofString(instr));
        } else {
            // 兼容旧语法：自动添加 relay: 前缀
            program.add(McIota.ofString("relay:" + instr));
        }
    }

    return program;
}
```

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
    List<IExecutable> program = SpellDiskItem.getProgram(stack);
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
    List<IData> dataStack = machine.getDataStackSnapshot();
    StringBuilder result = new StringBuilder();
    result.append("运行完成 (剩余 §e").append(machine.getRemainingOps()).append("§r 操作)");
    if (!dataStack.isEmpty()) {
        result.append(", 数据栈：§f").append(dataStackToString(dataStack));
    }

    source.sendSuccess(() -> Component.literal(result.toString()), true);
    return 1;
}
```

### 2. 外壳方块磁盘运行

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
List<IData> dataStack = machine.getDataStackSnapshot();
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
private static String dataStackToString(List<IData> dataStack) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < dataStack.size(); i++) {
        IData data = dataStack.get(i);
        if (i > 0) sb.append(", ");

        if (data instanceof McIota iota) {
            if (iota.isString()) {
                sb.append("\"").append(iota.asString()).append("\"");
            } else if (iota.isNumber()) {
                sb.append(iota.getValue());
            } else if (iota.isBoolean()) {
                sb.append(iota.asBoolean());
            } else {
                sb.append(iota.getType());
            }
        } else {
            sb.append(data.getType());
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
```

## 注意事项

1. **操作数限制**: ops 参数范围 1-10000，防止过度执行
2. **事故回调线程安全**: 事故处理器可能在主线程外调用，避免在回调中修改游戏状态
3. **程序反转**: `loadProgram()` 会自动反转程序列表，保证从左到右的执行顺序
4. **数据栈快照**: `getDataStackSnapshot()` 返回栈底到栈顶的顺序

## 验证步骤

```bash
# 构建验证
./gradlew build

# 测试命令 - JSON 语法（推荐）
/relay write hand '{"type":"relay:number","value":5};{"type":"relay:number","value":3};relay:add'
/relay run hand 10
# 预期输出：运行完成 (剩余 X 操作), 数据栈：8

# 测试命令 - 兼容旧语法
/relay write hand '5;3;add'
/relay run hand 10
# 注意：数字会被当作字符串，不推荐使用
```
