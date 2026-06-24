---
name: relay-state-machine-context
description: Relay 模组中通过 StateMachine 传递上下文数据的设计模式 - 保持 engine 纯粹性的同时允许操作访问世界相关数据
source: auto-skill
extracted_at: '2026-06-24T06:51:26.064Z'
---

# StateMachine 上下文传递模式

## 核心设计

通过 `StateMachine` 内部存储上下文数据，让操作可以通过 `executor` 获取世界相关数据，同时保持 `Executable` 接口的纯粹性。

## 架构优势

| 方面 | 直接传参方案 | 上下文容器方案 |
|------|-------------|---------------|
| **Executable 接口** | 需要修改签名 | ✅ 保持不变 |
| **所有 Iota 实现** | 需要改签名 | ✅ 保持不变 |
| **上下文类型** | 需要定义专门类 | ✅ `Map<String, Object>` 任意数据 |
| **engine 纯粹性** | ✅ 保持 | ✅ 保持 |
| **向后兼容** | ❌ 破坏性变更 | ✅ 非侵入式 |

## 实现步骤

### 1. StateMachine 添加上下文存储

```java
// engine/StateMachine.java
public class StateMachine {
    private final Map<String, Object> context = new HashMap<>();

    // 设置上下文数据
    public void setContext(String key, Object value) {
        context.put(key, value);
    }

    // 获取上下文数据（通用）
    public Object getContext(String key) {
        return context.get(key);
    }

    // 获取上下文数据（类型安全）
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key, Class<T> type) {
        Object value = context.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    // 检查是否存在上下文
    public boolean hasContext(String key) {
        return context.containsKey(key);
    }

    // 清空上下文
    public void clearContext() {
        context.clear();
    }
}
```

### 2. 调用处设置上下文

```java
// core/ShellTickHandler.java
public void tick(ShellContainer container) {
    // ... 其他逻辑

    if (initialized && coreCount > 0 && container.getStateMachine().isRunning()) {
        tickCounter++;
        if (tickCounter >= interval) {
            tickCounter = 0;

            // 设置上下文 - 传递世界交互器等信息给操作
            var stateMachine = container.getStateMachine();
            stateMachine.setContext("worldInteractor", container.getInteractorStack());
            stateMachine.setContext("shellContainer", container);
            // 可以传递任意数据：stateMachine.setContext("level", serverLevel);

            // 执行
            stateMachine.run(coreCount);

            // 可选：清空上下文
            stateMachine.clearContext();

            container.setChanged();
        }
    }
}
```

### 3. 操作从上下文获取数据

```java
// operations/PlaceBlockOp.java
@Override
public void execute(StateMachine executor) {
    // 检查是否有世界交互器
    if (!executor.hasContext("worldInteractor")) {
        executor.triggerMishap("操作需要世界交互器");
        return;
    }

    // 获取世界交互器（类型安全）
    ItemStack interactor = executor.getContext("worldInteractor", ItemStack.class);
    if (interactor == null || interactor.isEmpty()) {
        executor.triggerMishap("世界交互器为空");
        return;
    }

    // 执行世界交互逻辑
    // ...
}
```

### 4. 获取自身外壳容器（完整示例）

```java
// types/ContainerIota.java - 容器引用数据类型
public class ContainerIota extends Data {
    private final ShellContainer container;

    public ContainerIota(ShellContainer container) {
        super("relay:container", 0, DataSignature.builder()
                .output("relay:container")
                .build());
        this.container = container;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);
    }

    public ShellContainer getContainer() {
        return container;
    }
}

// operations/base/GetSelfOp.java - 获取自身容器操作
public class GetSelfOp extends Spell {
    public GetSelfOp() {
        super("relay:get_self", 1, 1, OperationSignature.builder()
                .output("relay:container")
                .build());
    }

    @Override
    public void execute(StateMachine executor) {
        // 从上下文中获取 shellContainer
        ShellContainer container = executor.getContext("shellContainer", ShellContainer.class);

        if (container == null) {
            executor.triggerMishap("无法获取自身容器：上下文缺失");
            return;
        }

        // 将容器作为 ContainerIota 压入数据栈
        executor.pushData(new ContainerIota(container));
    }
}
```

## 使用场景

### 场景 1：检查世界交互器

```java
@Override
public void execute(StateMachine executor) {
    if (!executor.hasContext("worldInteractor")) {
        executor.triggerMishap("需要世界交互器");
        return;
    }
    // 执行逻辑
}
```

### 场景 2：获取世界等级

```java
@Override
public void execute(StateMachine executor) {
    ServerLevel level = executor.getContext("level", ServerLevel.class);
    if (level == null) {
        executor.triggerMishap("无法访问世界");
        return;
    }
    // 执行世界相关操作
}
```

### 场景 3：获取执行玩家

```java
@Override
public void execute(StateMachine executor) {
    Player player = executor.getContext("player", Player.class);
    if (player == null) {
        executor.triggerMishap("无法获取玩家");
        return;
    }
    // 执行玩家相关操作
}
```

## 扩展性

### 传递多个上下文数据

```java
stateMachine.setContext("worldInteractor", interactorStack);
stateMachine.setContext("level", serverLevel);
stateMachine.setContext("player", player);
stateMachine.setContext("position", blockPos);
```

### 其他 Mod 扩展

其他 Mod 可以通过自定义上下文传递扩展数据：

```java
// 自定义 Mod 的 ShellContainer 扩展
stateMachine.setContext("myMod:energy", energyValue);
stateMachine.setContext("myMod:dimension", dimensionKey);
```

## 注意事项

1. **类型安全**：使用 `getContext(key, Class<T>)` 而不是直接转换，避免 `ClassCastException`
2. **空值检查**：`getContext()` 可能返回 `null`，使用前检查
3. **生命周期**：上下文在每次 `tick()` 时设置，执行后清空，避免数据污染
4. **键命名**：使用命名空间前缀避免冲突（如 `myMod:key`）
5. **运行时引用**：上下文中的数据（如 `ShellContainer`）是运行时引用，不会被序列化到磁盘
6. **数据类型包装**：如果需要将上下文数据压入数据栈，创建专门的 Iota 类型（如 `ContainerIota`）包装

## 文件位置

- `src/main/java/qdream/relay/engine/StateMachine.java` - 上下文存储
- `src/main/java/qdream/relay/core/ShellTickHandler.java` - 上下文设置
- `src/main/java/qdream/relay/types/ContainerIota.java` - 容器引用数据类型
- 操作实现中通过 `executor.getContext()` 获取

## 相关技能

- `relay-architecture-patterns` - Relay 模组的双栈执行模型
- `relay-executable-operation-layering` - engine/mc 分层架构
- `minecraft-26-1-2-api-changes` - Minecraft 26.1.2 API 适配
