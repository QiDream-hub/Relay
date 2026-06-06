# Iota 注册与运行框架

## 概述

Relay 使用基于栈的编程模型，核心数据单元称为 **Iota**（在 Minecraft 层称为 `McIota`）。Iota 是可执行的数据单元，既可以作为数据存储在数据栈，也可以作为指令在程序栈中执行。

---

## 核心架构

```
┌─────────────────────────────────────────────────────────────┐
│                    IExecutable 接口                          │
│  + getType(): String                                        │
│  + getValue(): Object                                       │
│  + execute(StateMachine): void                              │
│  + toJson(): JsonElement                                    │
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
┌───────────────┐                       ┌───────────────┐
│    McIota     │                       │  Operation    │
│ (数据实现)     │                       │  (操作实现)    │
└───────────────┘                       └───────────────┘
```

---

## 1. 类型系统

### 1.1 类型枚举 (`McIotaType`)

```
NUMBER    - 数字（Integer 或 Double）
BOOLEAN   - 布尔值
VECTOR    - 三维向量 (McVec3Adapter)
STRING    - 字符串（数据或操作 ID）
ENTITY    - 实体引用 (UUID)
LIST      - 列表 (List<IExecutable>)
NULL      - 空值
ANY       - 通配类型（用于操作签名）
```

### 1.2 类型注册表 (`IotaTypeRegistry`)

**位置**: `qdream.relay.engine.IotaTypeRegistry`（唯一注册表）

**职责**:
- 管理所有数据类型的 JSON 序列化/反序列化器
- 提供统一的 `toJson()` 和 `fromJson()` 方法

**注册方式**:
```java
IotaTypeRegistry.register(
    "relay:number",
    serializer,    // IData → JsonElement
    deserializer   // JsonObject → IData
);
```

**内置类型注册** (`McIotaTypes.register()`):

| 类型 ID | 说明 | JSON 格式 |
|--------|------|----------|
| `relay:number` | 数字 | `{"type":"relay:number","value":1}` 或 `{"type":"relay:number","value":1.5}` |
| `relay:boolean` | 布尔 | `{"type":"relay:boolean","value":true}` |
| `relay:string` | 字符串 | `{"type":"relay:string","value":"hello"}` |
| `relay:vector` | 向量 | `{"type":"relay:vector","value":{"x":1,"y":2,"z":3}}` |
| `relay:entity` | 实体 | `{"type":"relay:entity","value":"uuid-string"}` |
| `relay:list` | 列表 | `{"type":"relay:list","value":[...]}` |
| `relay:null` | 空值 | `{"type":"relay:null"}` |
| `relay:operation` | 操作引用 | `{"type":"relay:operation","op":"relay:add"}` |

---

## 2. 数据实现 (`McIota`)

**位置**: `qdream.relay.mc.McIota`

### 2.1 工厂方法

```java
McIota.ofInt(int value)
McIota.ofDouble(double value)
McIota.ofBoolean(boolean value)
McIota.ofVector(Object vec3)
McIota.ofString(String value)
McIota.ofEntity(UUID entityId)
McIota.ofList(List<IExecutable> value)
McIota.ofNull()
```

### 2.2 类型判断

```java
isNull(), isNumber(), isBoolean(), isVector(),
isString(), isEntity(), isList()
```

### 2.3 类型转换

```java
asInt(), asDouble(), asBoolean(), asString(),
asVector(), asEntity(), asList()
```

### 2.4 执行语义

```java
void execute(StateMachine executor) {
    if (type != STRING && type != LIST) {
        // 非字符串/列表 → 数据，压入数据栈
        executor.pushData(this);
    } else if (type == STRING) {
        // 字符串 → 操作 ID，执行操作
        executor.executeOperation((String) value);
    }
    // 列表 → 在 tick() 中处理，展开到程序栈
}
```

---

## 3. 执行引擎 (`StateMachine`)

**位置**: `qdream.relay.engine.StateMachine`

### 3.1 双栈结构

```
┌─────────────────┐     ┌─────────────────┐
│  Program Stack  │     │    Data Stack   │
│  (IExecutable)  │     │     (IData)     │
├─────────────────┤     ├─────────────────┤
│  {"op":"add"}   │     │  5 (number)     │
│  2 (number)     │     │  3 (number)     │
│  1 (number)     │     │                 │
└─────────────────┘     └─────────────────┘
```

### 3.2 程序加载

```java
public void loadProgram(List<IExecutable> program) {
    // 反转列表后压栈，保证从左到右执行
    List<IExecutable> reversed = new ArrayList<>(program);
    Collections.reverse(reversed);
    programStack.clear();
    for (IExecutable iota : reversed) {
        programStack.push(iota);
    }
}
```

**示例**: 程序 `[1, 2, add]` 加载后栈序：
```
┌─────────────────┐
│  add (top)      │  ← 先执行
│  2              │
│  1 (bottom)     │  ← 后执行
└─────────────────┘
```

### 3.3 Tick 执行循环

```java
public void tick(int ops) {
    remainingOps = ops;
    
    while (remainingOps > 0 && !programStack.isEmpty()) {
        IExecutable top = programStack.pop();
        
        if ("string".equals(top.getType())) {
            // 操作 ID → 执行操作
            String opId = (String) top.getValue();
            executeOperation(opId);
        } else if ("list".equals(top.getType())) {
            // 列表 → 反转后展开到程序栈
            List<IExecutable> list = (List<IExecutable>) top.getValue();
            // ...反转并压栈
            remainingOps--;
        } else if (/* 其他数据类型 */) {
            // 数据 → 宽容规则：自动压入数据栈
            dataStack.push(top);
            remainingOps--;
        } else {
            // 未知类型 → 事故
            triggerMishap("未知的栈顶类型：" + top.getType());
        }
    }
}
```

### 3.4 操作执行

```java
public void executeOperation(String opId) {
    OperationRegistry.getEntry(opId).ifPresentOrElse(entry -> {
        // 1. 检查世界交互器
        if (entry.requiresWorldInteractor() && !hasWorldInteractor) {
            triggerMishap("操作 " + opId + " 需要世界交互器");
            return;
        }

        // 2. 检查操作数
        if (remainingOps < entry.getCost()) {
            triggerMishap("操作数不足");
            return;
        }

        // 3. 执行操作
        entry.getOperation().execute(this);
        remainingOps -= entry.getCost();
        
    }, () -> triggerMishap("未知操作：" + opId));
}
```

### 3.5 事故处理

```java
public void triggerMishap(String reason) {
    programStack.clear();   // 清空程序栈
    dataStack.clear();      // 清空数据栈
    remainingOps = 0;       // 终止执行
    
    if (mishapHandler != null) {
        mishapHandler.onMishap(reason);  // 回调通知
    }
}
```

---

## 4. 操作注册表 (`OperationRegistry`)

**位置**: `qdream.relay.engine.OperationRegistry`

### 4.1 操作条目结构

```java
public class OperationEntry {
    private final String id;           // 操作 ID (如 "relay:add")
    private final IExecutable operation; // 操作实现
    private final String signature;    // 签名 (如 "NN→N")
    private final int cost;            // 操作成本
    private final boolean requiresWorldInteractor;
}
```

### 4.2 注册 API

```java
// 链式注册
OperationRegistry.register("relay:add", new AddOp())
        .signature("NN→N")
        .cost(1)
        .requiresWorldInteractor(false)
        .register();
```

### 4.3 操作分类

| 分类 | 操作示例 |
|------|---------|
| 基础栈操作 | `relay:pop`, `relay:dup`, `relay:swap` |
| 算术操作 | `relay:add`, `relay:sub`, `relay:mul`, `relay:div` |
| 逻辑操作 | `relay:and`, `relay:or`, `relay:not` |
| 比较操作 | `relay:eq`, `relay:lt`, `relay:gt` |
| 控制流 | `relay:eval`, `relay:if`, `relay:stop` |
| 通信操作 | `relay:send`, `relay:recv`, `relay:peek` |
| 列表操作 | `relay:list-append`, `relay:list-get`, ... |
| 世界交互 | `relay:get-block`, `relay:set_block`, ... |

---

## 5. 初始化流程

### 5.1 启动链路

```
Relay.onInitialize()
    ├── RelayItems.init()          // 物品注册
    ├── RelayBlocks.init()         // 方块注册
    ├── OperationsInit.register()  // 操作注册表
    │       ├── 基础操作
    │       ├── 算术/逻辑/比较
    │       ├── 控制流/通信
    │       ├── ListOperationsInit.register()
    │       └── WorldOperationsInit.register()
    └── McIotaTypes.register()     // 类型序列化器 (注册到 engine.IotaTypeRegistry)
```

### 5.2 操作注册示例

```java
public class OperationsInit {
    public static void register() {
        // 基础栈操作
        OperationRegistry.register("relay:pop", new PopOp())
                .requiresWorldInteractor(false)
                .register();

        // 算术操作
        OperationRegistry.register("relay:add", new AddOp())
                .requiresWorldInteractor(false)
                .register();

        // ... 其他操作
    }
}
```

---

## 6. 数据流示例

### 6.1 程序：`1 + 2`

**程序表示** (JSON):
```json
[
  {"type":"relay:number","value":1},
  {"type":"relay:number","value":2},
  {"type":"relay:operation","op":"relay:add"}
]
```

**执行过程**:

| Tick | 程序栈 (top→) | 数据栈 (top→) | 说明 |
|------|--------------|--------------|------|
| 初始 | `add, 2, 1` | `[]` | 程序加载后反转 |
| 1 | `add, 2` | `[1]` | 弹出 1，压入数据栈 |
| 2 | `add` | `[2, 1]` | 弹出 2，压入数据栈 |
| 3 | `[]` | `[3]` | 执行 add，弹出 1,2，压入 3 |

### 6.2 操作实现示例 (`AddOp`)

```java
public class AddOp implements IExecutable {
    @Override
    public void execute(StateMachine machine) {
        IData a = machine.popData();
        IData b = machine.popData();
        
        if (a.isNumber() && b.isNumber()) {
            double result = a.asDouble() + b.asDouble();
            machine.pushData(McIota.ofDouble(result));
        } else {
            throw new RuntimeException("类型错误");
        }
    }
}
```

---

## 7. 设计决策

### 7.1 统一 JSON 格式

所有 `IExecutable` 实现使用相同的 JSON 结构：
- 必须包含 `type` 字段
- 数据包含 `value` 字段
- 操作包含 `op` 字段

### 7.2 双栈模型

- **程序栈**: 存储待执行的 `IExecutable`
- **数据栈**: 存储临时计算结果

优势：
- 清晰的执行/数据分离
- 支持嵌套列表展开
- 简化操作实现（只操作数据栈）

### 7.3 宽容规则

程序栈弹出数据时，不报错，而是自动压入数据栈。这允许程序混合编写数据和操作，无需显式 `push` 指令。

### 7.4 无原生循环

循环通过列表自引用 + `eval` 实现：
```
[程序, 条件, [程序, 条件, eval, if], eval, if]
```

### 7.5 单一类型注册表

只有一个 `engine.IotaTypeRegistry` 作为类型注册表，`McIotaTypes.register()` 将所有内置类型的序列化器注册到该注册表。

---

## 8. 扩展点

### 8.1 注册自定义类型

```java
// 1. 定义类型
public class MyIota extends McIota {
    // ...
}

// 2. 注册序列化器
IotaTypeRegistry.register("mymod:custom",
    data -> {
        // 序列化逻辑
    },
    json -> {
        // 反序列化逻辑
    }
);
```

### 8.2 注册自定义操作

```java
OperationRegistry.register("mymod:myop", new MyOp())
        .signature("N→N")
        .cost(1)
        .requiresWorldInteractor(false)
        .register();
```

---

## 9. 相关文件索引

| 文件 | 路径 | 职责 |
|------|------|------|
| `IExecutable` | `engine/IExecutable.java` | 可执行单元接口 |
| `IData` | `engine/IData.java` | 数据接口 + 类型注册表委托 |
| `IotaTypeRegistry` | `engine/IotaTypeRegistry.java` | 类型注册表（唯一） |
| `McIota` | `mc/McIota.java` | Minecraft 层 Iota 实现 |
| `McIotaType` | `mc/McIotaType.java` | 类型枚举 |
| `McIotaTypes` | `mc/McIotaTypes.java` | 内置类型序列化器注册 |
| `StateMachine` | `engine/StateMachine.java` | 状态机执行器 |
| `OperationRegistry` | `engine/OperationRegistry.java` | 操作注册表 |
| `OperationsInit` | `operations/OperationsInit.java` | 操作初始化 |
