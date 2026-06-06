---
name: relay-architecture-patterns
description: Relay 模组的架构模式，包括双栈状态机、统一 Executable 接口、engine/mc 分层架构
source: auto-skill
extracted_at: '2026-06-07T00:00:00.000Z'
---

# Relay 模组架构模式

Relay 是一个基于栈的编程模组，为 Minecraft 添加可视化法术编程系统。以下是其核心架构模式。

## 0. Engine/MC 两层架构 (2026-06-07 精简后)

核心执行引擎与 Minecraft 解耦，支持版本迁移和独立测试。

**当前架构**：
1. `engine/` 包：纯 Java 引擎（`Executable` 接口，`StateMachine`）
2. `mc/` 包：Minecraft 适配层（`OperationRegistry`, `NbtSerializer`）
3. `types/` 包：Iota 类型实现（直接实现 `Executable`）
4. `mc/base/` 包：抽象基类（`Operation`, `Spell`, `Data`）

```
src/main/java/qdream/relay/
├── engine/                    # 纯 Java 引擎（零 MC 依赖）
│   ├── IData.java             # 数据接口（getType(), getValue(), toJson()）
│   ├── IExecutable.java       # 可执行接口（继承 IData + execute()）
│   ├── StateMachine.java      # 状态机核心（使用 IExecutable）
│   ├── DataWrapper.java       # 数据包装器（语法糖核心）
│   ├── OperationRegistry.java # 操作注册表
│   └── OperationSignature.java# 签名（使用字符串表示类型）
│
├── types/                     # 类型实现层（mod 具体类型）
│   ├── NumberIota.java        # 数字类型（实现 IData）
│   ├── StringIota.java        # 字符串类型（实现 IData）
│   ├── BooleanIota.java       # 布尔类型（实现 IData）
│   ├── VectorIota.java        # 向量类型（实现 IData）
│   ├── EntityIota.java        # 实体类型（实现 IData）
│   ├── NullIota.java          # 空值类型（实现 IData，单例）
│   ├── Operation.java         # 操作引用（实现 IExecutable）
│   ├── ProgramBlock.java      # 程序块（实现 IExecutable）
│   ├── Iotas.java             # 工厂类（返回 IExecutable）
│   └── IotaSerializers.java   # 序列化器注册
│
├── mc/                        # MC 适配层
│   ├── McVec3Adapter.java     # Vec3 包装类
│   ├── NbtSerializer.java     # NBT 序列化工具
│   └── StateMachineNbtSerializer.java
│
├── core/                      # MC 集成层（保留）
│   ├── CommunicationSystem.java
│   ├── ShellContainer.java
│   ├── ShellTickHandler.java
│   └── EnergySystem.java
│
└── operations/                # 操作实现（依赖 engine 和 types）
```

### 单一类型注册表设计

| 组件 | 职责 | 依赖 |
|------|------|------|
| `engine.IotaTypeRegistry` | 唯一类型注册表，管理所有 JSON 序列化器 | 纯 Java |
| `IData.TypeRegistry` | 委托接口，简化注册调用 | 委托到 `IotaTypeRegistry` |
| `mc.IotaSerializers.register()` | 注册所有内置类型的序列化器 | 调用 `IData.TypeRegistry.register()` |
| `mc.NumberIota.toJson()` | 序列化实例（每个类型自己实现） | 无 |

### 关键设计决策

| 特性 | 设计 | 理由 |
|------|------|------|
| IData 接口 | `getType()` (返回 String), `getValue()`, `toJson()` | engine 包不依赖任何具体类型定义 |
| IExecutable 接口 | 继承 IData，添加 `execute(StateMachine)` | 可执行单元也是数据，可存储在栈中 |
| 类型字符串化 | 类型用 `"relay:number"`, `"relay:string"` 等字符串表示 | 避免 engine 依赖枚举，支持跨 mod 扩展 |
| 独立类型类 | 每个类型独立实现（`NumberIota`, `StringIota` 等） | 类型安全，代码清晰 |
| 类型只实现 IData | 纯数据不实现 `IExecutable` | 语义清晰：数据不是操作 |
| DataWrapper 语法糖 | `DataWrapper<IData>` 实现 `IExecutable`，execute 时自动压栈 | 统一程序栈类型 (`IExecutable`)，同时保持数据/操作分离 |
| 工厂类返回 IExecutable | `Iotas.number(1)` 返回 `DataWrapper<NumberIota>` | 调用者无需关心包装细节 |
| 世界操作适配 | PlaceBlockOp/GetBlockOp 使用 `McVec3Adapter.getVec3()` | 在需要 MC API 时转换 |

### DataWrapper 语法糖核心

```java
// engine 包 - 数据包装器
public class DataWrapper implements IExecutable {
    private final IData data;

    public DataWrapper(IData data) {
        this.data = data;
    }

    @Override
    public String getType() {
        return data.getType();
    }

    @Override
    public Object getValue() {
        return data.getValue();
    }

    @Override
    public JsonElement toJson() {
        return data.toJson();
    }

    @Override
    public void execute(StateMachine executor) {
        // 语法糖：纯数据执行时自动压入数据栈
        executor.pushData(data);
    }

    public IData getData() {
        return data;
    }
}
```

### 工厂类使用 DataWrapper

```java
// types 包 - 工厂类
public final class Iotas {
    private Iotas() {}

    // 返回 IExecutable（内部是 DataWrapper）
    public static IExecutable number(double value) {
        return new DataWrapper(new NumberIota(value));
    }

    public static IExecutable number(int value) {
        return new DataWrapper(new NumberIota(value));
    }

    public static IExecutable booleanIota(boolean value) {
        return new DataWrapper(new BooleanIota(value));
    }

    public static IExecutable string(String value) {
        return new DataWrapper(new StringIota(value));
    }

    // Operation 和 ProgramBlock 直接实现 IExecutable，无需包装
    public static IExecutable operation(String opId) {
        return new Operation(opId);
    }

    public static IExecutable list(List<IExecutable> items) {
        return new ProgramBlock(items);
    }

    // 类型转换辅助（从 IData 提取）
    public static NumberIota asNumber(IData data) {
        if (data instanceof NumberIota n) return n;
        throw new IllegalArgumentException("期望 number 类型，实际为：" + data.getType());
    }
    // ... 其他 asXxx() 方法
}
```

### IData/IExecutable 接口设计

```java
// engine 包 - 纯 Java 接口
public interface IData {
    String getType();  // 返回 "relay:number", "relay:string" 等
    Object getValue();
}

public interface IExecutable extends IData {
    void execute(StateMachine executor);
}
```

### 独立类型类实现模式

```java
// types 包 - 数字类型（只实现 IData）
public class NumberIota implements IData {
    private final double value;

    public NumberIota(double value) { this.value = value; }
    public NumberIota(int value) { this.value = value; }

    @Override
    public String getType() { return "relay:number"; }

    @Override
    public Object getValue() { return value; }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "relay:number");
        json.addProperty("value", value);
        return json;
    }

    // 类型转换方法
    public double asDouble() { return value; }
    public int asInt() { return (int) value; }
    public boolean isInteger() { return value == (int) value; }
}

// types 包 - 字符串类型（只实现 IData）
public class StringIota implements IData {
    private final String value;

    public StringIota(String value) { this.value = value; }

    @Override
    public String getType() { return "relay:string"; }

    @Override
    public Object getValue() { return value; }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "relay:string");
        json.addProperty("value", value);
        return json;
    }

    public String asString() { return value; }
}

// types 包 - 操作引用（实现 IExecutable）
public class Operation implements IExecutable {
    private final String opId;

    public Operation(String opId) { this.opId = opId; }

    @Override
    public String getType() { return "relay:operation"; }

    @Override
    public Object getValue() { return opId; }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "relay:operation");
        json.addProperty("op", opId);
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.executeOperation(opId);  // 执行操作
    }

    public String getOpId() { return opId; }
}

// types 包 - 程序块（列表，实现 IExecutable）
public class ProgramBlock implements IExecutable {
    private final List<IExecutable> items;

    public ProgramBlock(List<IExecutable> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    @Override
    public String getType() { return "relay:list"; }

    @Override
    public Object getValue() { return items; }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "relay:list");
        JsonArray array = new JsonArray();
        for (IExecutable item : items) {
            array.add(item.toJson());
        }
        json.add("value", array);
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        // 列表反转后压入程序栈，保证从左到右的执行顺序
        List<IExecutable> reversed = new ArrayList<>(items);
        Collections.reverse(reversed);
        for (IExecutable item : reversed) {
            executor.pushProgram(item);
        }
    }

    public List<IExecutable> getItems() { return new ArrayList<>(items); }
}
```

### StateMachine 使用接口

```java
public class StateMachine {
    private final Deque<IExecutable> programStack;  // 程序栈存可执行单元
    private final Deque<IData> dataStack;           // 数据栈存数据

    public IData popData() {
        if (dataStack.isEmpty()) {
            triggerMishap("数据栈为空");
            return null;
        }
        return dataStack.pop();
    }

    public void pushData(IData iota) {
        dataStack.push(iota);
    }

    public void tick(int ops) {
        while (remainingOps > 0 && !programStack.isEmpty()) {
            IExecutable top = programStack.pop();

            if ("string".equals(top.getType())) {
                // 操作 ID → 执行
                executeOperation((String) top.getValue());
            } else if ("list".equals(top.getType())) {
                // 列表 → 反转后压入程序栈
                @SuppressWarnings("unchecked")
                List<IExecutable> list = (List<IExecutable>) top.getValue();
                // ...
            } else {
                // 数据 → 自动压入数据栈
                dataStack.push(top);
            }
            remainingOps--;
        }
    }
}
```

### 操作中的类型转换模式

```java
// 操作中 popData 返回 IData，需要转换为 McIota
public void execute(StateMachine executor) {
    IData bData = executor.popData();
    if (!(bData instanceof McIota b)) return;

    IData aData = executor.popData();
    if (!(aData instanceof McIota a)) return;

    if (!a.isNumber() || !b.isNumber()) {
        throw new IllegalArgumentException("需要数值参数");
    }

    double result = a.asDouble() + b.asDouble();
    executor.pushData(McIota.ofDouble(result));
}
```

### NBT 序列化模式

```java
// mc 包 - 序列化工具类
public class NbtSerializer {
    public static final NbtSerializer INSTANCE = new NbtSerializer();

    public CompoundTag serialize(McIota iota) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", iota.getType());  // 使用字符串

        switch (iota.getType()) {
            case "number" -> {
                if (iota.getValue() instanceof Double) {
                    tag.putDouble("value", iota.asDouble());
                } else {
                    tag.putInt("value", iota.asInt());
                }
            }
            case "vector" -> {
                Object vec = iota.asVector();
                if (vec instanceof McVec3Adapter) {
                    Vec3 v = ((McVec3Adapter) vec).getVec3();
                    tag.putDouble("x", v.x);
                    tag.putDouble("y", v.y);
                    tag.putDouble("z", v.z);
                }
            }
            // ...
        }
        return tag;
    }

    public McIota deserialize(CompoundTag tag) {
        String typeName = tag.getString("type").orElse("unknown");
        return switch (typeName) {
            case "vector" -> McIota.ofVector(new McVec3Adapter(new Vec3(...)));
            // ...
        };
    }
}
```

## 1. 双栈执行模型 (重构后)

状态机维护两个独立的栈：程序栈 (IExecutable) 和数据栈 (IData)。

```java
public class StateMachine {
    private final Deque<IExecutable> programStack = new ArrayDeque<>();
    private final Deque<IData> dataStack = new ArrayDeque<>();

    // 程序栈：存储可执行单元（操作 ID 字符串、列表）
    // 数据栈：存储临时值（数字、布尔、向量等）
}
```

### Tick 执行循环

```java
public void tick(int ops) {
    remainingOps = ops;

    while (remainingOps > 0 && !programStack.isEmpty()) {
        IExecutable top = programStack.pop();

        if ("string".equals(top.getType())) {
            // 操作 ID → 执行回调
            executeOperation((String) top.getValue());
        } else if ("list".equals(top.getType())) {
            // 列表 → 反转后压入程序栈（子程序调用）
            @SuppressWarnings("unchecked")
            List<IExecutable> list = (List<IExecutable>) top.getValue();
            List<IExecutable> reversed = new ArrayList<>(list);
            Collections.reverse(reversed);
            for (IExecutable iota : reversed) programStack.push(iota);
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
| 类型字符串判断 | 使用 `"string".equals(top.getType())` | engine 包不依赖枚举 |

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

## 3. 操作签名设计 (重构后)

OperationSignature 使用字符串表示类型，避免 engine 包依赖枚举。

```java
public class OperationSignature {
    private final List<String> inputs;   // ["number", "number"]
    private final List<String> outputs;  // ["number"]

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> inputs = new ArrayList<>();
        private final List<String> outputs = new ArrayList<>();

        public Builder input(String type) {
            inputs.add(type);
            return this;
        }

        public Builder output(String type) {
            outputs.add(type);
            return this;
        }

        public OperationSignature build() {
            return new OperationSignature(inputs, outputs);
        }
    }
}

// 使用示例
public OperationSignature getSignature() {
    return OperationSignature.builder()
            .input("number")
            .input("number")
            .output("number")
            .build();
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

### 操作中的类型错误报告模式（2026-06-06 更新）

所有操作在类型检查失败时必须调用 `triggerMishap()` 向玩家报告错误，而不是静默返回。

```java
// ❌ 错误：静默失败，玩家无法知道问题
public void execute(StateMachine executor) {
    IData bData = executor.popData();
    if (!(bData instanceof NumberIota b)) return;  // 静默返回
    // ...
}

// ✅ 正确：报告错误到聊天栏
public void execute(StateMachine executor) {
    IData bData = executor.popData();
    if (bData == null) return;
    if (!(bData instanceof NumberIota b)) {
        executor.triggerMishap("操作 relay:add 期望 number 类型，实际为：" + bData.getType());
        return;
    }
    // ...
}
```

### 错误消息格式

```
操作 <操作 ID> 期望 <期望类型> 类型，实际为：<实际类型>
```

示例：
- `操作 relay:add 期望 number 类型，实际为：relay:null`
- `操作 relay:if 期望 boolean 类型，实际为：relay:string`
- `操作 relay:list_get 期望 number 类型，实际为：relay:boolean`

### 所有操作的类型检查模板

```java
@Override
public void execute(StateMachine executor) {
    // 1. pop 数据
    IData argData = executor.popData();
    
    // 2. 检查 null（pop 失败已触发 mishap）
    if (argData == null) return;
    
    // 3. 检查类型，错误时触发 mishap
    if (!(argData instanceof ExpectedType arg)) {
        executor.triggerMishap("操作 relay:xxx 期望 xxx 类型，实际为：" + argData.getType());
        return;
    }
    
    // 4. 正常执行逻辑
    // ...
}
```

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

## 7. 项目结构 (重构后)

```
src/main/java/qdream/relay/
├── engine/                    # 纯 Java 引擎（无 MC 依赖）
│   ├── StateMachine.java      # 状态机执行器
│   ├── Iota.java              # 类型系统
│   ├── IotaType.java          # 类型枚举
│   ├── Vector3.java           # 向量接口
│   ├── StackOperation.java    # 操作接口
│   ├── OperationRegistry.java # 操作注册表
│   ├── OperationSignature.java # 操作签名
│   └── serialization/         # 序列化抽象
├── mc/                        # MC 适配层
│   ├── McVec3Adapter.java     # Vec3 适配
│   ├── NbtSerializer.java     # NBT 序列化
│   └── StateMachineNbtSerializer.java
├── core/                      # MC 集成层
│   ├── CommunicationSystem.java # 通信系统
│   ├── ShellContainer.java    # 容器接口
│   ├── ShellTickHandler.java  # Tick 调度
│   ├── EnergySystem.java      # 能量系统
│   └── ShellRegistry.java     # 外壳注册表
├── items/
│   ├── RelayItems.java
│   ├── ComputingCoreItem.java
│   ├── SpellDiskItem.java
│   ├── EnergyModuleItem.java
│   └── ToolShellItem.java
├── blocks/
│   ├── RelayBlocks.java
│   ├── RelayBlockEntities.java
│   ├── ShellBlock.java
│   └── entity/ShellBlockEntity.java
├── entities/
│   └── EntityShell.java       # 实体外壳
├── operations/                # 操作实现（依赖 engine）
│   ├── OperationsInit.java
│   ├── base/                  # 基础操作
│   ├── arithmetic/            # 算术操作
│   ├── logic/                 # 逻辑操作
│   ├── control/               # 控制流
│   ├── communication/         # 通信操作
│   ├── list/                  # 列表操作
│   └── world/                 # 世界交互操作
└── networking/
    ├── RelayServerNetworking.java
    └── payloads/
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
