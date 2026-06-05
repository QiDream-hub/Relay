---
name: minecraft-26-1-2-command-registration
description: Minecraft 26.1.2 Fabric 命令注册模式和权限检查 API 变更
source: auto-skill
extracted_at: '2026-06-06T00:00:00.000Z'
---

# Minecraft 26.1.2 命令注册模式

## Fabric 命令注册方式

在 26.1.2 (1.21+) 中，使用 Fabric API 注册命令的标准模式：

```java
// 在主类中使用 CommandRegistrationCallback
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

@Override
public void onInitialize() {
    // 其他初始化...
    
    // 注册命令
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
        RelayCommands.register(dispatcher);
    });
}
```

## 命令注册类结构

```java
package qdream.relay.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RelayCommands {

    private static final SimpleCommandExceptionType NO_DISK = new SimpleCommandExceptionType(
            Component.literal("手中没有法术磁盘")
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("relay")
                .build();

        // 子命令
        LiteralCommandNode<CommandSourceStack> writeSpell = Commands.literal("write_spell")
                .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("program", StringArgumentType.greedyString())
                                .executes(RelayCommands::writeSpellToHand)
                        )
                )
                .build();

        dispatcher.getRoot().addChild(root);
        root.addChild(writeSpell);
    }

    private static int writeSpellToHand(CommandContext<CommandSourceStack> context) 
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        
        // 命令逻辑...
        
        source.sendSuccess(() -> Component.literal("成功消息"), true);
        return 1;
    }
}
```

## 权限检查 API 变更（26.1.2 重要）

### 问题

在 26.1.2 版本中，`PlayerList.isOp()` 方法的参数类型发生了变化：

```java
// 旧版本 (1.20.1 及之前)
boolean isOp(UUID playerId);
boolean isOp(GameProfile profile);

// 26.1.2 新版本
boolean isOp(NameAndId nameAndId);
```

`NameAndId` 是一个记录类，包含玩家名称和 UUID：
```java
public record NameAndId(Component name, UUID id) {}
```

### 常见错误

```java
// ❌ 错误：Component 无法转换为 NameAndId
source.getServer().getPlayerList().isOp(player.getName());

// ❌ 错误：UUID 无法转换为 NameAndId  
source.getServer().getPlayerList().isOp(player.getUUID());
```

### 解决方案

**方案 1：创建 NameAndId 对象（推荐）**

```java
var nameAndId = new net.minecraft.world.entity.player.Player.NameAndId(
    player.getName(), 
    player.getUUID()
);
boolean isOp = source.getServer().getPlayerList().isOp(nameAndId);
```

**方案 2：简化权限检查（本项目采用）**

对于开发/测试环境，可以简化权限检查，在命令执行时再检查：

```java
// 注册时不检查权限
LiteralCommandNode<CommandSourceStack> root = Commands.literal("relay")
        .build();

// 在执行时检查（如果需要）
private static int executeCommand(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    var player = source.getPlayerOrException();
    
    // 如果需要权限检查，使用 NameAndId
    var nameAndId = new net.minecraft.world.entity.player.Player.NameAndId(
        player.getName(), 
        player.getUUID()
    );
    if (!source.getServer().getPlayerList().isOp(nameAndId)) {
        throw new SimpleCommandExceptionType(
            Component.literal("需要管理员权限")
        ).create();
    }
    
    // 命令逻辑...
}
```

**方案 3：使用 Brigadier 的 requires 谓词**

```java
LiteralCommandNode<CommandSourceStack> root = Commands.literal("relay")
        .requires(source -> {
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                return false;
            }
            var nameAndId = new Player.NameAndId(player.getName(), player.getUUID());
            return source.getServer().getPlayerList().isOp(nameAndId);
        })
        .build();
```

## 常用参数类型

```java
// 字符串参数
Commands.argument("name", StringArgumentType.string())
Commands.argument("name", StringArgumentType.word())
Commands.argument("program", StringArgumentType.greedyString())

// 数字参数
Commands.argument("count", IntegerArgumentType.integer())
Commands.argument("value", DoubleArgumentType.doubleArg())

// 方块坐标
Commands.argument("pos", BlockPosArgument.blockPos())

// 物品
Commands.argument("item", ItemArgument.item())
```

## 建议提示（Suggests）

为参数添加自动补全建议：

```java
Commands.argument("target", StringArgumentType.word())
    .suggests((ctx, builder) -> {
        builder.suggest("hand");
        builder.suggest("shell");
        return builder.buildFuture();
    })
```

## 异常类型

```java
// 简单异常
private static final SimpleCommandExceptionType ERROR_NOT_FOUND = 
    new SimpleCommandExceptionType(Component.literal("未找到目标"));

// 动态异常
private static final DynamicCommandExceptionType ERROR_COUNT = 
    new DynamicCommandExceptionType(
        name -> Component.literal("数量无效：" + name)
    );

// 抛出异常
throw ERROR_NOT_FOUND.create();
throw ERROR_COUNT.create(someValue);
```

## 完整示例

```java
// /relay write_spell hand "push 5; push 3; add"
private static int writeSpellToHand(CommandContext<CommandSourceStack> context) 
        throws CommandSyntaxException {
    CommandSourceStack source = context.getSource();
    var player = source.getPlayerOrException();
    ItemStack stack = player.getMainHandItem();

    if (!(stack.getItem() instanceof SpellDiskItem)) {
        throw NO_DISK.create();
    }

    String programStr = StringArgumentType.getString(context, "program");
    List<IExecutable> program = parseProgram(programStr);
    SpellDiskItem.setProgram(stack, program);

    source.sendSuccess(() -> 
        Component.literal("已写入法术程序：§e" + program.size() + "§r 个指令"), 
        true
    );
    return program.size();
}
```

## 验证步骤

```bash
./gradlew build
```

在服务器上测试命令：
```bash
/relay write_spell hand "push 1; push 2; add"
/relay read hand
/relay clear hand
```
