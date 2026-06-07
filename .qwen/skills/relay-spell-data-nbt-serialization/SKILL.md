---
name: relay-spell-data-nbt-serialization
description: Relay 模组中 Spell 和 Data 的 NBT 序列化设计 - 数据多实例存储状态，操作单例从注册表获取
source: auto-skill
extracted_at: '2026-06-07T06:30:00.000Z'
---

# Relay Spell 和 Data 的 NBT 序列化设计

## 核心设计原则

**2026-06-07 确认**：在 Relay 的架构中，`Data` 和 `Spell` 都需要多实例存储能力，但序列化策略不同：

| 类型 | 是否需要多实例 | 序列化策略 | 反序列化策略 |
|------|---------------|-----------|-------------|
| **Data** (数据) | ✅ 需要 | 保存 `id + value` | 创建新实例 + 恢复状态 |
| **Spell** (操作) | ❌ 不需要 | 只保存 `id` | 从注册表获取单例 |

## 类层次结构

```
Executable (engine 包 - 纯接口，零 MC 依赖)
└── Operation (mc/base 包 - 抽象类，扩展 Executable)
    ├── id: String          // 操作 ID，如 "relay:add"
    ├── cost: int           // 操作成本
    └── execute()           // 执行逻辑
    │
    ├── Data (mc/base 包 - 抽象类，多实例)
    │   ├── toNbt()         // 抽象方法，子类必须实现
    │   ├── fromNbt()       // 抽象方法，返回新实例
    │   │
    │   ├── NumberIota      // value: double
    │   ├── BooleanIota     // value: boolean
    │   ├── StringIota      // value: String
    │   ├── VectorIota      // value: Vec3
    │   ├── EntityIota      // value: UUID
    │   ├── NullIota        // 单例
    │   └── ListIota        // value: List<Executable>
    │
    └── Spell (mc/base 包 - 抽象类，单例操作)
        ├── signature       // 操作签名（输入/输出类型）
        ├── toNbt()         // 默认实现：空（只保存 id）
        ├── fromNbt()       // 默认实现：返回 this（单例）
        │
        ├── AddOp
        ├── SubOp
        ├── PopOp
        └── ...
```

## 设计原因

### 1. Engine/MC 解耦

用户设计意图：
> "因为我想让 engine 和 mc 解绑所以定义了 Operation 扩展 Executable"

**架构优势**：
- `engine.StateMachine` 只依赖 `Executable` 接口，纯 Java 实现
- `Operation` 在 `mc/base` 包，添加 Minecraft 相关元数据（`id`, `cost`, `signature`）
- `Data` 和 `Spell` 都可以放入程序栈和数据栈（都实现 `Executable`）

### 2. Data 需要多实例的原因

数据需要存储不同的值：
```java
NumberIota(5)   // 值 5
NumberIota(10)  // 值 10
```

**序列化实现**（以 `NumberIota` 为例）：
```java
public class NumberIota extends Data {
    private final double value;

    @Override
    public void toNbt(CompoundTag tag) {
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
            return new NumberIota(intOpt.get());  // 创建新实例
        } else {
            return new NumberIota(tag.getDouble("value").orElse(0.0));
        }
    }
}
```

### 3. Spell 使用单例的原因

操作是无状态的，所有实例共享同一逻辑：
```java
AddOp()  // 执行加法逻辑，不存储状态
AddOp()  // 另一个实例，逻辑相同
```

**序列化实现**：
```java
public abstract class Spell extends Operation {
    protected final OperationSignature signature;

    public Spell(String id, int cost, OperationSignature signature) {
        super(id, cost);
        this.signature = signature;
    }

    @Override
    public void toNbt(CompoundTag tag) {
        // 默认实现：空（操作是单例，只保存 id，已在 Operation 中保存）
    }

    @Override
    public Spell fromNbt(CompoundTag tag) {
        // 操作是单例，返回自身
        return this;
    }
}
```

## OperationRegistry 序列化逻辑

### 序列化方法

```java
public static Optional<CompoundTag> serializeToNbt(Executable exec) {
    String id = ((Operation) exec).getId();
    CompoundTag tag = new CompoundTag();
    tag.putString("id", id);  // 所有类型都保存 id

    if (exec instanceof Data data) {
        data.toNbt(tag);  // 数据：额外保存 value
        return Optional.of(tag);
    } else if (exec instanceof Spell spell) {
        spell.toNbt(tag);  // 操作：默认空实现
        return Optional.of(tag);
    } else {
        // 其他 Executable 类型，只保存 id
        return Optional.of(tag);
    }
}
```

### 反序列化方法

```java
public static Optional<Executable> deserializeFromNbt(CompoundTag tag) {
    String id = tag.getString("id").orElse("");

    // 1. 尝试作为数据类型反序列化（多实例）
    Optional<Data> dataOpt = createData(id);
    if (dataOpt.isPresent()) {
        return Optional.of(dataOpt.get().fromNbt(tag));  // 创建新实例 + 恢复状态
    }

    // 2. 尝试作为操作获取（单例）
    Optional<Executable> opOpt = get(id);
    if (opOpt.isPresent()) {
        return opOpt;  // 从注册表获取单例
    }

    // 3. 未知类型
    return Optional.empty();
}
```

## ShellBlockEntity 和 EntityShell 持久化

### 保存程序列表

```java
// ShellBlockEntity.java
public void saveProgramToDisk() {
    ItemStack diskStack = inventory[DISK_SLOT];
    if (!diskStack.isEmpty()) {
        CompoundTag programTag = new CompoundTag();
        
        // 序列化程序栈
        ListTag programList = new ListTag();
        for (Executable exe : stateMachine.getProgramStackSnapshot()) {
            OperationRegistry.serializeToNbt(exe).ifPresent(programList::add);
        }
        programTag.put("program", programList);
        
        // 序列化数据栈
        ListTag dataList = new ListTag();
        for (Executable data : stateMachine.getDataStackSnapshot()) {
            OperationRegistry.serializeToNbt(data).ifPresent(dataList::add);
        }
        programTag.put("dataStack", dataList);
        
        // 保存到磁盘 NBT
        diskStack.getOrCreateTag().put("SpellProgram", programTag);
    }
}
```

### 加载程序列表

```java
public void loadProgramFromDisk() {
    ItemStack diskStack = inventory[DISK_SLOT];
    if (!diskStack.isEmpty()) {
        CompoundTag diskTag = diskStack.getTag();
        if (diskTag != null && diskTag.contains("SpellProgram")) {
            CompoundTag programTag = diskTag.getCompound("SpellProgram");
            
            // 反序列化程序栈
            ListTag programList = programTag.getList("program").orElse(new ListTag());
            List<Executable> program = new ArrayList<>();
            for (Tag element : programList) {
                OperationRegistry.deserializeFromNbt((CompoundTag) element)
                    .ifPresent(program::add);
            }
            stateMachine.loadProgram(program);
            
            // 反序列化数据栈
            ListTag dataList = programTag.getList("dataStack").orElse(new ListTag());
            for (Tag element : dataList) {
                OperationRegistry.deserializeFromNbt((CompoundTag) element)
                    .ifPresent(data -> stateMachine.pushData(data));
            }
        }
    }
}
```

## 关键设计决策

### 1. 为什么 Spell 的 fromNbt() 返回 this？

**原因**：操作是无状态的单例，不需要创建新实例。

```java
// AddOp.java
public class AddOp extends Spell {
    public AddOp() {
        super("relay:add", 1, signature);
    }
    
    // fromNbt() 继承 Spell 的默认实现，返回 this
    // 因为所有 AddOp 实例都是相同的，不需要反序列化创建新实例
}
```

**实际使用**：反序列化时从注册表获取单例：
```java
// OperationRegistry.deserializeFromNbt()
Optional<Executable> opOpt = get(id);  // 返回注册表中的单例
if (opOpt.isPresent()) {
    return opOpt;  // 不使用 fromNbt()
}
```

### 2. 为什么 Data 的 fromNbt() 返回新实例？

**原因**：数据需要存储不同的值，必须创建新实例。

```java
// NumberIota.java
@Override
public Data fromNbt(CompoundTag tag) {
    var intOpt = tag.getInt("value");
    if (intOpt.isPresent()) {
        return new NumberIota(intOpt.get());  // 新实例，值可能不同
    }
}
```

### 3. 数据类型工厂注册表

为了支持反序列化，需要注册数据类型工厂：

```java
// RelayOperations.java
private static void registerDataTypes() {
    OperationRegistry.registerData("relay:number", () -> new NumberIota(0));
    OperationRegistry.registerData("relay:boolean", () -> new BooleanIota(false));
    OperationRegistry.registerData("relay:string", () -> new StringIota(""));
    OperationRegistry.registerData("relay:vector", () -> new VectorIota(new Vec3(0, 0, 0)));
    OperationRegistry.registerData("relay:entity", () -> new EntityIota(new UUID(0, 0)));
    OperationRegistry.registerData("relay:null", NullIota::new);
    OperationRegistry.registerData("relay:list", () -> new ListIota(new ArrayList<>()));
}
```

**工厂方法作用**：
1. 提供一个默认值实例（用于创建新实例的模板）
2. `fromNbt()` 被调用时，实际是调用工厂创建的新实例的 `fromNbt()` 方法

### 4. ListIota 的递归序列化

`ListIota` 存储 `List<Executable>`，需要递归调用注册表：

```java
public class ListIota extends Data {
    private final List<Executable> value;

    @Override
    public void toNbt(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (Executable item : value) {
            // 递归调用注册表序列化每个元素
            OperationRegistry.serializeToNbt(item).ifPresent(listTag::add);
        }
        tag.put("value", listTag);
    }

    @Override
    public Data fromNbt(CompoundTag tag) {
        var valueOpt = tag.getList("value");
        if (valueOpt.isPresent()) {
            ListTag listTag = valueOpt.get();
            List<Executable> list = new ArrayList<>();
            for (Tag element : listTag) {
                if (element instanceof CompoundTag compoundTag) {
                    // 递归调用注册表反序列化每个元素
                    OperationRegistry.deserializeFromNbt(compoundTag).ifPresent(list::add);
                }
            }
            return new ListIota(list);
        } else {
            return new ListIota(new ArrayList<>());
        }
    }
}
```

## 注意事项

### 1. Minecraft 26.1.2 API 变化

在 26.1.2 版本中，`CompoundTag` 的 getter 方法返回 `Optional` 类型：

```java
// ✅ 正确：处理 Optional
Optional<Integer> intOpt = tag.getInt("value");
if (intOpt.isPresent()) {
    int value = intOpt.get();
}

// 或使用 orElse
int value = tag.getInt("value").orElse(0);
```

### 2. 类型 ID 格式

所有类型 ID 必须使用 `modid:type` 格式：
- `"relay:number"`
- `"relay:add"`
- `"relay:list"`

避免与其他 mod 冲突。

### 3. 未知类型处理

反序列化未知类型时返回 `Optional.empty()`：

```java
// 未知类型
return Optional.empty();
```

调用者应处理此情况：
```java
OperationRegistry.deserializeFromNbt(tag).ifPresent(exec -> {
    stateMachine.pushData(exec);
});
```

### 4. 初始化顺序

必须确保在序列化/反序列化前完成注册：

```java
// Relay.java
public class Relay implements ModInitializer {
    @Override
    public void onInitialize() {
        // 1. 注册数据类型
        // 2. 注册操作
        // 3. 注册 NBT 序列化器
        RelayOperations.register();
        
        // 4. 注册其他内容
        RelayBlocks.register();
        RelayItems.register();
    }
}
```

## 与 JSON 序列化系统的关系

Relay 有两套序列化系统：

| 特性 | JSON 序列化 | NBT 序列化 |
|------|------------|-----------|
| 用途 | 法术程序字符串解析（命令/GUI） | 物品/方块数据持久化 |
| 注册表 | `IotaTypeRegistry` | `OperationRegistry` |
| 格式 | JSON 文本 | Minecraft NBT |
| 数据类型 | 多实例 | 多实例 |
| 操作类型 | 单例（从注册表获取） | 单例（从注册表获取） |

两套系统设计相似，但服务于不同的使用场景。
