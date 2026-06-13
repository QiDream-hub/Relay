---
name: relay-executable-operation-layering
description: Relay 模组的 Executable/Operation 分层架构设计 - engine 层纯粹接口，mc 层抽象基类提供序列化
source: auto-skill
extracted_at: '2026-06-13T00:00:00.000Z'
---

# Relay Executable/Operation 分层架构设计

## 核心设计原则

**Engine 层保持纯粹**：`engine` 包对 Minecraft 零依赖，支持独立测试和版本迁移。

**Mc 层提供抽象基类**：`mc/base` 包中的 `Operation` 类提供序列化实现，减少子类重复代码。

## 当前架构（2026-06-13）

### 分层结构

```
┌─────────────────────────────────────────────────────────┐
│ engine/ (纯 Java，零 MC 依赖)                             │
│ ├── Executable.java     // 纯粹接口：execute(), getCost()│
│ └── StateMachine.java   // 双栈执行器，依赖 Executable  │
└─────────────────────────────────────────────────────────┘
                          ↓ 实现
┌─────────────────────────────────────────────────────────┐
│ mc/base/ (Minecraft 适配层)                              │
│ └── Operation.java      // 抽象基类，实现 Executable    │
│     - 存储 id, cost 字段                                 │
│     - 提供 toJson/fromJson/toNbt/fromNbt 模板方法        │
│     - 依赖 net.minecraft.nbt.CompoundTag                │
│     - 依赖 com.google.gson.JsonObject                   │
└─────────────────────────────────────────────────────────┘
                          ↓ 继承
┌─────────────────────────────────────────────────────────┐
│ types/ 和 operations/ (具体实现)                         │
│ ├── NumberIota extends Data extends Operation           │
│ ├── ListIota extends Data extends Operation             │
│ ├── AddOp extends Operation                             │
│ └── ...                                                 │
└─────────────────────────────────────────────────────────┘
```

### Executable 接口（engine 层）

```java
// engine/Executable.java - 纯粹接口，无 MC 依赖
public interface Executable {
    /**
     * 执行此可执行单元
     * 数据：将自己压入数据栈
     * 操作：执行计算或世界交互
     */
    void execute(StateMachine executor);
    
    /**
     * 执行时消耗的资源
     */
    int getCost();
}
```

### Operation 基类（mc 层）

```java
// mc/base/Operation.java - 抽象基类，实现 Executable
public abstract class Operation implements Executable {
    protected final String id;
    protected final int cost;

    public Operation(String id, int cost) {
        this.id = id;
        this.cost = cost;
    }

    // 提供 ID 访问
    public String getId() {
        return id;
    }

    @Override
    public int getCost() {
        return cost;
    }

    // ========== JSON 序列化模板方法 ==========
    
    /**
     * 序列化为 JSON（写入 id 字段）
     * 子类 override 以添加自身字段
     */
    public void toJson(JsonObject json) {
        json.addProperty("id", id);
    }

    /**
     * 从 JSON 反序列化（返回自身，适用于无状态单例操作）
     * 子类 override 以读取自身字段
     */
    public Operation fromJson(JsonObject json) {
        return this;
    }

    // ========== NBT 序列化模板方法 ==========
    
    /**
     * 序列化为 NBT（写入 id 字段）
     * 子类 override 以添加自身字段
     */
    public void toNbt(CompoundTag tag) {
        tag.putString("id", id);
    }

    /**
     * 从 NBT 反序列化（返回自身，适用于无状态单例操作）
     * 子类 override 以读取自身字段
     */
    public Operation fromNbt(CompoundTag tag) {
        return this;
    }
}
```

### Data 基类（mc 层）

```java
// mc/base/Data.java - 数据类型基类
public abstract class Data extends Operation {
    public Data(String id, int cost) {
        super(id, cost);
    }

    /**
     * 从 NBT 反序列化，返回新的 Data 实例
     * 数据类型必须 override 此方法
     */
    @Override
    public abstract Data fromNbt(CompoundTag tag);

    /**
     * 从 JSON 反序列化，返回新的 Data 实例
     * 数据类型必须 override 此方法
     */
    @Override
    public abstract Data fromJson(JsonObject json);
}
```

### 数据类型实现示例

```java
// types/NumberIota.java
public class NumberIota extends Data {
    private final double value;

    public NumberIota(double value) {
        super("relay:number", 0);
        this.value = value;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);  // 数据执行时将自己压入数据栈
    }

    @Override
    public void toNbt(CompoundTag tag) {
        super.toNbt(tag);  // 写入 id
        if (isInteger()) {
            tag.putInt("value", asInt());
        } else {
            tag.putDouble("value", asDouble());
        }
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        var intOpt = tag.getInt("value");
        if (intOpt.isPresent()) {
            return new NumberIota(intOpt.get());
        } else {
            return new NumberIota(tag.getDouble("value").orElse(0.0));
        }
    }

    @Override
    public void toJson(JsonObject json) {
        super.toJson(json);  // 写入 id
        json.addProperty("value", value);
    }

    @Override
    public Data fromJson(JsonObject json) {
        if (json.has("value")) {
            return new NumberIota(json.get("value").getAsDouble());
        }
        return new NumberIota(0);
    }
}
```

### 操作实现示例

```java
// operations/arithmetic/AddOp.java
public class AddOp extends Operation {
    public AddOp() {
        super("relay:add", 1);
    }

    @Override
    public void execute(StateMachine executor) {
        Executable bData = executor.popData();
        Executable aData = executor.popData();
        
        if (!(bData instanceof NumberIota b) || !(aData instanceof NumberIota a)) {
            executor.triggerMishap("操作 relay:add 期望 number 类型");
            return;
        }
        
        executor.pushData(new NumberIota(a.asDouble() + b.asDouble()));
    }
}
```

## 设计决策讨论（2026-06-13）

### 用户提议

> "mc 实现层可以不使用 Executable 接口，而使用 mc 层定义的 Operation 基类"

### 分析

**优点**：
- 减少接口冗余，`Operation` 已经定义了 `id` 和 `cost` 字段
- 集中序列化逻辑在 `Operation` 基类

**问题**：
1. **`StateMachine` 依赖 `Executable`**：
   ```java
   private final Deque<Executable> programStack;
   private final Deque<Executable> dataStack;
   ```
   如果 mc 层不使用 `Executable`，需要让 `StateMachine` 依赖 `Operation`，这会破坏分层。

2. **`engine` 层的纯粹性**：
   - `engine` 包目前**没有 Minecraft 依赖**
   - `Operation` 类依赖 `net.minecraft.nbt.CompoundTag` 和 `com.google.gson.JsonObject`
   - 如果 `StateMachine` 使用 `Operation`，就需要引入 MC 依赖

3. **类型系统割裂**：
   - `types` 包下的 Iota 类型都继承 `Data` → `Operation`
   - 如果 `engine` 层不使用 `Executable`，整个执行模型需要重构

### 最终结论

**保持当前架构**：

| 层次 | 职责 | 依赖 |
|------|------|------|
| `engine/Executable` | 纯粹接口，定义 `execute()` 和 `getCost()` | 无 |
| `mc/base/Operation` | 抽象基类，实现 `Executable`，提供序列化模板 | MC NBT + Gson |
| `types/*Iota` | 数据类型，继承 `Data` → `Operation` | 依赖 Operation |
| `operations/*Op` | 操作实现，继承 `Operation` | 依赖 Operation |

**理由**：
1. `engine` 层保持对 Minecraft 的零依赖，便于独立测试和版本迁移
2. `StateMachine` 的双栈需要统一的 `Executable` 类型
3. `Operation` 作为 mc 层的抽象基类，提供序列化实现，减少重复代码
4. 当前架构已经工作良好，无需重构

## 序列化统一格式

所有 `Executable` 实现都使用相同的 JSON 结构：

```json
// 数据字面量
{"id":"relay:number","value":1}

// 操作
{"id":"relay:add"}

// 列表
{"id":"relay:list","value":[
  {"id":"relay:number","value":1},
  {"id":"relay:number","value":2},
  {"id":"relay:add"}
]}
```

**设计理由**：
- 统一序列化逻辑：所有 `Executable` 实现都使用相同的 JSON 结构
- 操作作为特殊类型：操作类型 (`relay:add`) 与数据类型并列
- 支持自定义数据类型扩展：其他 mod 可注册自己的类型
- 完全限定名避免冲突：`relay:number` vs `othermod:number`

## 相关文件

- `engine/Executable.java` - 纯粹接口定义
- `engine/StateMachine.java` - 双栈执行器
- `mc/base/Operation.java` - 抽象基类，提供序列化模板
- `mc/base/Data.java` - 数据类型基类
- `types/NumberIota.java` - 数据类型实现示例
- `operations/arithmetic/AddOp.java` - 操作实现示例
- `mc/OperationRegistry.java` - 统一注册表
