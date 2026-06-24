---
name: relay-shell-owner-context
description: Relay 模组中为 Shell 添加主人属性和上下文系统扩展的实现模式
source: auto-skill
extracted_at: '2026-06-24T07:23:41.288Z'
---

# Relay Shell 主人属性与上下文系统扩展

## 核心设计模式

### 1. ShellContainer 接口扩展

为 Shell 容器添加主人（owner）属性的标准模式：

```java
// ShellContainer.java
public interface ShellContainer {
    // 所有者（主人）管理
    Entity getOwner();
    void setOwner(Entity owner);
    default boolean hasOwner() {
        return getOwner() != null;
    }
}
```

**实现要点**：
- 在 `ShellBlockEntity`、`EntityShell`、`ToolShellContainer` 中都添加 `Entity owner` 字段
- 所有实现类都必须实现 `getOwner()` / `setOwner()` 方法
- 伪容器（如 `DummyShellContainer`）返回 `null`

### 2. NBT 序列化（26.1.2 ValueInput/ValueOutput）

使用 UUID 字符串存储所有者信息：

```java
// ShellBlockEntity.java - 保存
@Override
protected void saveAdditional(ValueOutput output) {
    // 保存所有者信息
    if (owner != null) {
        output.putString("owner", owner.getUUID().toString());
    }
}

// 加载
@Override
protected void loadAdditional(ValueInput input) {
    // 加载所有者信息
    String uuidStr = input.getString("owner").orElse("");
    if (!uuidStr.isEmpty()) {
        try {
            java.util.UUID uuid = java.util.UUID.fromString(uuidStr);
            if (level != null && !level.isClientSide()) {
                owner = level.getEntity(uuid);
            }
        } catch (IllegalArgumentException e) {
            // UUID 格式错误，忽略
        }
    }
}
```

### 3. 上下文系统传递 ShellContainer

在 `ShellTickHandler.tick()` 中设置上下文：

```java
// ShellTickHandler.java
public void tick(ShellContainer container) {
    if (container.isClientSide()) return;
    if (!container.isEnabled()) return;

    // ... 其他逻辑 ...

    if (initialized && coreCount > 0 && container.getStateMachine().isRunning()) {
        tickCounter++;
        if (tickCounter >= interval) {
            tickCounter = 0;

            // 设置上下文 - 传递世界交互器和 ShellContainer 给操作
            var stateMachine = container.getStateMachine();
            stateMachine.setContext("worldInteractor", container.getInteractorStack());
            stateMachine.setContext("shellContainer", container);

            // 执行
            stateMachine.run(coreCount);

            // 清空上下文
            stateMachine.clearContext();
        }
    }
}
```

**设计理由**：
- 操作通过上下文访问 ShellContainer，保持 engine 层纯粹性
- 每次 tick 前设置，tick 后清空，避免内存泄漏
- 支持多种上下文数据（worldInteractor、shellContainer 等）

### 3.1 命令中的上下文注入

在命令执行时也必须设置 `shellContainer` 上下文，否则 `get_self`、`get_owner` 等操作会失败：

```java
// RelayCommands.java
private static int runShellWithInteractor(CommandContext<CommandSourceStack> context) {
    // ... 获取 shell ...
    
    StateMachine machine = new StateMachine();
    
    // 如果启用世界交互器，设置上下文
    if (useWorldInteractor) {
        machine.setContext("worldInteractor", shell.getInteractorStack());
    }
    
    // 设置 shellContainer 上下文，支持 get_self、get_owner 等操作
    machine.setContext("shellContainer", shell);
    
    machine.loadProgram(program);
    machine.run(ops);
}
```

**手持磁盘的伪容器实现**：

对于手持磁盘运行命令，需要创建伪 `ShellContainer` 实现：

```java
// RelayCommands.java
private static ShellContainer createHandContainer(ItemStack stack) {
    return new ShellContainer() {
        @Override
        public ItemStack getInventorySlot(int slot) {
            return slot == 1 ? stack : ItemStack.EMPTY;
        }

        @Override
        public void setInventorySlot(int slot, ItemStack itemStack) {
        }

        @Override
        public StateMachine getStateMachine() {
            return null;
        }

        @Override
        public int getCoreCount() {
            return 1;
        }

        @Override
        public int getInterval() {
            return 1;
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public void setInitialized(boolean initialized) {
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void setEnabled(boolean enabled) {
        }

        @Override
        public int getEnergy() {
            return 1000;
        }

        @Override
        public void setEnergy(int energy) {
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean isClientSide() {
            return false;
        }

        @Override
        public Entity getOwner() {
            return null;  // 手持磁盘没有主人
        }

        @Override
        public void setOwner(Entity owner) {
        }
    };
}

// 在 runHandWithInteractor 中使用
machine.setContext("shellContainer", createHandContainer(stack));
```

**重要**：所有执行法术程序的入口点都必须设置 `shellContainer` 上下文！

### 4. EntityIota 通用实体类型改造

支持存储 Entity 引用和 UUID 双重模式，支持 null 实体：

```java
// EntityIota.java
public class EntityIota extends Data {
    private final UUID entityId;
    private final Entity entityRef;  // 运行时引用

    private EntityIota(UUID entityId, Entity entityRef) {
        super("relay:entity", 0, DataSignature.builder()
                .output("relay:entity")
                .build());
        this.entityId = entityId;
        this.entityRef = entityRef;
    }

    /**
     * 从 Entity 创建 EntityIota（保持实体引用）
     * @param entity 实体，可以为 null
     * @return EntityIota 实例
     */
    public static EntityIota from(Entity entity) {
        if (entity == null) {
            return new EntityIota(null, null);
        }
        return new EntityIota(entity.getUUID(), entity);
    }

    /**
     * 从 UUID 创建 EntityIota（用于反序列化，不保持引用）
     */
    public static EntityIota fromUuid(UUID entityId) {
        return new EntityIota(entityId, null);
    }

    public UUID asEntity() {
        return entityId;
    }

    public Entity getEntity() {
        return entityRef;
    }

    public boolean hasEntity() {
        return entityRef != null;
    }

    public boolean isNull() {
        return entityId == null && entityRef == null;
    }
}
```

**使用模式**：
- `EntityIota.from(entity)` - 从实体创建（保持引用，支持 null）
- `EntityIota.fromUuid(uuid)` - 从 UUID 创建（用于反序列化）
- `entityIota.isNull()` - 检查是否是 null 实体

### 5. 上下文访问操作实现

创建操作从上下文中获取数据：

```java
// GetOwnerOp.java
public class GetOwnerOp extends Spell {
    public GetOwnerOp() {
        super("relay:get_owner", 1, 1, OperationSignature.builder()
                .output("relay:entity")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 从上下文中获取 shellContainer
        ShellContainer container = executor.getContext("shellContainer", ShellContainer.class);

        if (container == null) {
            executor.triggerMishap("无法获取容器：上下文缺失");
            return;
        }

        // 获取所有者实体
        Entity owner = container.getOwner();

        // 将所有者作为 EntityIota 压入数据栈
        executor.pushData(EntityIota.from(owner));
    }
}
```

**类似操作**：
- `GetSelfOp` - 获取自身 ShellContainer（返回 `ContainerIota`）
- `GetWorldInteractorOp` - 获取世界交互器（返回 `boolean`）
- `IsPlayerOp` - 检测实体是否是玩家（新增）
- `SendMessageOp` - 向玩家发送消息（新增）

### 6. 数据类型注册

为新的数据类型注册：

```java
// RelayOperations.java
private static void registerDataTypes() {
    // ... 其他类型 ...
    OperationRegistry.register("relay:container",
            new OperationRegistry.DataEntry(() -> new ContainerIota(null)));
}

private static void registerOperations() {
    // ... 其他操作 ...
    OperationRegistry.register("relay:get_self",
            new OperationRegistry.OpEntry(new GetSelfOp()));
    OperationRegistry.register("relay:get_owner",
            new OperationRegistry.OpEntry(new GetOwnerOp()));
    OperationRegistry.register("relay:is_player",
            new OperationRegistry.OpEntry(new IsPlayerOp()));
    OperationRegistry.register("relay:send_message",
            new OperationRegistry.OpEntry(new SendMessageOp()));
}
```

### 7. 新增操作实现

#### IsPlayerOp - 检测实体是否是玩家

```java
// operations/base/IsPlayerOp.java
public class IsPlayerOp extends Spell {
    public IsPlayerOp() {
        super("relay:is_player", 1, 1, OperationSignature.builder()
                .input("relay:entity")
                .output("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        var entityExe = executor.popData();
        if (!(entityExe instanceof EntityIota entityIota)) {
            executor.triggerMishap("期望 entity 类型");
            return;
        }

        Entity entity = entityIota.getEntity();
        boolean isPlayer = (entity instanceof Player);
        executor.pushData(new BooleanIota(isPlayer));
    }
}
```

#### SendMessageOp - 向玩家发送消息

```java
// operations/communication/SendMessageOp.java
public class SendMessageOp extends Spell {
    public SendMessageOp() {
        super("relay:send_message", 2, 1, OperationSignature.builder()
                .input("relay:entity")
                .input("relay:string")
                .output("relay:boolean")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 弹出实体
        var entityExe = executor.popData();
        if (!(entityExe instanceof EntityIota entityIota)) {
            executor.triggerMishap("期望 entity 类型");
            return;
        }

        // 弹出消息字符串
        var msgExe = executor.popData();
        if (!(msgExe instanceof StringIota stringIota)) {
            executor.triggerMishap("期望 string 类型");
            return;
        }

        Entity entity = entityIota.getEntity();
        String message = stringIota.asString();

        if (entity instanceof Player player) {
            player.sendSystemMessage(Component.literal(message));
            executor.pushData(new BooleanIota(true));
        } else {
            executor.pushData(new BooleanIota(false));
        }
    }
}
```

## 完整实现清单

### 需要修改的文件
1. `core/ShellContainer.java` - 接口添加 owner 方法
2. `blocks/entity/custom/ShellBlockEntity.java` - 实现 owner 字段和 NBT 序列化
3. `entities/EntityShell.java` - 实现 owner 字段
4. `items/ToolShellContainer.java` - 实现 owner 字段
5. `screen/ShellScreenHandler.java` - `DummyShellContainer` 实现 owner 方法
6. `types/EntityIota.java` - 改造为通用实体类型
7. `types/ContainerIota.java` - 新建（存储 ShellContainer 引用）
8. `operations/base/GetSelfOp.java` - 新建（获取自身）
9. `operations/base/GetOwnerOp.java` - 新建（获取主人）
10. `mc/RelayOperations.java` - 注册新操作和类型
11. `core/ShellTickHandler.java` - 设置上下文

## 使用示例

```
# 获取自身 Shell
relay:get_self

# 获取主人
relay:get_owner

# 检查是否有主人
relay:get_owner relay:is_null relay:if ...

# 检查主人是否是玩家
relay:get_owner relay:is_player relay:if ...

# 向主人发送消息
relay:get_owner "Hello!" relay:send_message

# 检查是否是自己拥有的 Shell
relay:get_owner relay:get_self relay:eq relay:if ...

# 条件发送消息
relay:get_owner relay:is_player relay:if {
    relay:get_owner "主人在线!" relay:send_message
}
```

## 注意事项

1. **上下文生命周期**：上下文在 tick 开始时设置，tick 结束时清空
2. **运行时引用**：`EntityIota` 的 `entityRef` 字段只在运行时有效，不会被序列化
3. **NBT 序列化**：使用 UUID 字符串存储，反序列化时通过 `level.getEntity(uuid)` 恢复引用
4. **空值处理**：所有访问上下文的代码都要检查 `null`，避免空指针异常
5. **客户端/服务端**：owner 只在服务端有效，客户端 `level.getEntity()` 可能返回 null
