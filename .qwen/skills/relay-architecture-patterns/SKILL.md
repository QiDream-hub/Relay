---
name: relay-architecture-patterns
description: Relay 模组的核心架构模式，包括双栈状态机、操作注册链式 API、通信系统设计
source: auto-skill
extracted_at: '2026-05-30T09:52:48.181Z'
---

# Relay 模组架构模式

Relay 是一个基于栈的编程模组，为 Minecraft 添加可视化法术编程系统。以下是其核心架构模式。

## 1. 双栈执行模型

状态机维护两个独立的栈：程序栈和数据栈。

```java
public class StateMachine {
    private final Deque<Iota> programStack = new ArrayDeque<>();
    private final Deque<Iota> dataStack = new ArrayDeque<>();
    
    // 程序栈：存储可执行单元（操作 ID 字符串、列表）
    // 数据栈：存储临时值（数字、布尔、向量等）
}
```

### Tick 执行循环

```java
public void tick(int ops) {
    remainingOps = ops;
    
    while (remainingOps > 0 && !programStack.isEmpty()) {
        Iota top = programStack.pop();
        
        if (top.isString()) {
            // 操作 ID → 执行回调
            executeOperation(opId);
        } else if (top.isList()) {
            // 列表 → 反转后压入程序栈（子程序调用）
            List<Iota> reversed = new ArrayList<>(top.asList());
            Collections.reverse(reversed);
            for (Iota iota : reversed) programStack.push(iota);
        } else {
            // 数据 → 自动压入数据栈（宽容规则）
            dataStack.push(top);
        }
        remainingOps--;
    }
}
```

### 关键设计决策

| 特性 | 设计 | 理由 |
|------|------|------|
| 程序加载反转 | 从左到右执行顺序 | 符合人类阅读习惯 |
| 列表自动展开 | 支持嵌套子程序 | 实现 `eval` 控制流 |
| 数据宽容压栈 | 非操作值自动压入数据栈 | 简化编程模型 |
| 每 Tick 操作数限制 | 防止卡顿 | 性能保护 |

## 2. 操作注册链式 API

使用流式构建器模式注册操作，支持元数据配置。

```java
// 注册 API
OperationRegistry.register("add", new AddOp())
        .requiresWorldInteractor(false)
        .cost(1)
        .register();

// 链式构建器
public static class ChainBuilder {
    private final String id;
    private final StackOperation operation;
    private OperationSignature signature;
    private int cost = 1;
    private boolean requiresWorldInteractor = false;
    
    public ChainBuilder signature(OperationSignature sig) { ... }
    public ChainBuilder cost(int cost) { ... }
    public ChainBuilder requiresWorldInteractor(boolean requires) { ... }
    public void register() { ... }
}
```

### 操作条目结构

```java
public class OperationEntry {
    private final StackOperation operation;  // 执行回调
    private OperationSignature signature;    // 类型签名（输入/输出类型）
    private int cost;                        // 操作消耗
    private boolean requiresWorldInteractor; // 是否需要世界交互权限
}
```

### 执行时检查

```java
private void executeOperation(String opId) {
    OperationRegistry.getEntry(opId).ifPresentOrElse(entry -> {
        // 检查世界交互器
        if (entry.requiresWorldInteractor() && !hasWorldInteractor) {
            triggerMishap("操作 " + opId + " 需要世界交互器");
            return;
        }
        
        // 检查操作数预算
        if (remainingOps < entry.getCost()) {
            triggerMishap("操作 " + opId + " 需要 " + entry.getCost() + " 操作数");
            return;
        }
        
        entry.getOperation().execute(this);
        remainingOps -= entry.getCost();
    }, () -> triggerMishap("未知操作：" + opId));
}
```

## 3. Iota 类型系统

所有栈中值封装为 `Iota`，支持 7 种类型。

```java
public enum IotaType { NUMBER, BOOLEAN, VECTOR, STRING, ENTITY, LIST, NULL, ANY }

public class Iota {
    private final IotaType type;
    private final Object value;
    
    // 工厂方法
    public static Iota ofNumber(Number value);
    public static Iota ofBoolean(boolean value);
    public static Iota ofVector(Vec3 value);
    public static Iota ofString(String value);
    public static Iota ofEntity(UUID uuid);
    public static Iota ofList(List<Iota> value);
    public static Iota ofNull();
    
    // NBT 序列化
    public CompoundTag toNbt();
    public static Iota fromNbt(CompoundTag tag);
}
```

### NBT 序列化要点

```java
public CompoundTag toNbt() {
    CompoundTag tag = new CompoundTag();
    tag.putString("type", type.name());
    
    switch (type) {
        case NUMBER -> {
            if (value instanceof Double) tag.putDouble("value", (Double) value);
            else tag.putInt("value", ((Number) value).intValue());
        }
        case BOOLEAN -> tag.putBoolean("value", (Boolean) value);
        case VECTOR -> {
            Vec3 v = (Vec3) value;
            tag.putDouble("x", v.x);
            tag.putDouble("y", v.y);
            tag.putDouble("z", v.z);
        }
        case ENTITY -> tag.putString("value", ((UUID) value).toString());
        case LIST -> {
            ListTag list = new ListTag();
            for (Iota iota : (List<Iota>) value) list.add(iota.toNbt());
            tag.put("value", list);
        }
    }
    return tag;
}
```

## 4. 通信系统设计

全局频道 Map 支持跨维度消息传递。

```java
public class CommunicationSystem {
    private static final Map<Integer, Queue<Iota>> CHANNELS = new ConcurrentHashMap<>();
    public static final int MAX_QUEUE_SIZE = 1000;
    
    public static boolean send(int channel, Iota data) {
        Queue<Iota> queue = CHANNELS.computeIfAbsent(channel, k -> new ConcurrentLinkedQueue<>());
        if (queue.size() >= MAX_QUEUE_SIZE) return false;
        return queue.offer(data);
    }
    
    public static Iota recv(int channel) {
        Queue<Iota> queue = CHANNELS.get(channel);
        return (queue == null || queue.isEmpty()) ? Iota.ofNull() : queue.poll();
    }
    
    public static Iota peek(int channel) {
        Queue<Iota> queue = CHANNELS.get(channel);
        return (queue == null || queue.isEmpty()) ? Iota.ofNull() : queue.peek();
    }
}
```

### 设计要点

| 特性 | 实现 | 理由 |
|------|------|------|
| 线程安全 | `ConcurrentHashMap` + `ConcurrentLinkedQueue` | 多线程访问 |
| 队列容量限制 | 1000 | 防止内存溢出 |
| 空频道返回 null | `Iota.ofNull()` | 统一错误处理 |
| 全局共享 | 静态 Map | 跨维度通信 |

## 5. 事故（Mishap）处理

统一错误处理机制，清空状态并回调。

```java
public interface MishapHandler {
    void onMishap(String reason);
}

public void triggerMishap(String reason) {
    programStack.clear();
    dataStack.clear();
    remainingOps = 0;
    
    if (mishapHandler != null) {
        mishapHandler.onMishap(reason);
    }
}
```

### 触发条件

- 未知操作 ID
- 类型不匹配
- 操作数不足
- 栈溢出
- 缺少世界交互器
- 队列已满（通信系统）

## 6. 外壳方块实体架构

```java
public class ShellBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStack[] inventory = new ItemStack[4];
    
    // 插槽定义
    public static final int CORE_SLOT = 0;      // 运算核心
    public static final int DISK_SLOT = 1;      // 法术磁盘
    public static final int ENERGY_SLOT = 2;    // 能量模块
    public static final int INTERACTOR_SLOT = 3; // 世界交互器
    
    // Tick 逻辑
    public static void tick(Level world, BlockPos pos, BlockState state, ShellBlockEntity entity) {
        if (world.isClientSide()) return;
        
        entity.updateCoreState();  // 更新核心数量和 interval
        if (!entity.initialized) entity.tryInitialize();  // 尝试加载程序
        if (entity.initialized && entity.coreCount > 0) {
            entity.tickCounter++;
            if (entity.tickCounter >= entity.interval) {
                entity.tickCounter = 0;
                // 执行状态机 tick
            }
        }
    }
}
```

## 7. 项目结构

```
src/main/java/qdream/relay/
├── core/
│   ├── Iota.java              # 类型系统
│   ├── IotaType.java          # 类型枚举
│   ├── StateMachine.java      # 状态机执行器
│   ├── OperationRegistry.java # 操作注册表
│   ├── OperationSignature.java # 操作签名
│   ├── StackOperation.java    # 操作接口
│   └── CommunicationSystem.java # 通信系统
├── items/
│   ├── RelayItems.java        # 物品注册
│   ├── ComputingCoreItem.java
│   ├── SpellDiskItem.java
│   └── EnergyModuleItem.java
├── blocks/
│   ├── RelayBlocks.java       # 方块注册
│   ├── RelayBlockEntities.java # 方块实体注册
│   ├── ShellBlock.java        # 外壳方块
│   └── entity/ShellBlockEntity.java
├── operations/
│   ├── OperationsInit.java    # 操作初始化
│   ├── base/                  # 基础操作
│   ├── arithmetic/            # 算术操作
│   ├── logic/                 # 逻辑操作
│   ├── control/               # 控制流
│   └── communication/         # 通信操作
└── networking/
    ├── RelayServerNetworking.java
    └── payloads/              # 网络包定义
```

## 8. 待完成的关键功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 外壳 GUI 容器 | P0 | 4 插槽容器实现 |
| 法术磁盘程序加载/保存 | P0 | NBT 与状态机集成 |
| 核心合并机制 | P1 | 相邻核心扫描累加 |
| 能量系统 | P1 | 紫水晶能量检测 |
| 世界交互操作库 | P1 | placeBlock, breakBlock 等 |
| 网络同步 | P1 | C2S/S2C 包处理 |
| 类型推导引擎 | P2 | 编辑器实时验证 |
