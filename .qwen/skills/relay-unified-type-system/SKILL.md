---
name: relay-unified-type-system
description: Relay 模组统一类型系统架构——所有 Iota 类型直接实现 Executable 接口，消除纯数据与可执行的二元对立
source: auto-skill
extracted_at: '2026-06-06T09:45:00.000Z'
---

# Relay 统一类型系统架构

2026-06-06 重构后，Relay 采用统一类型系统：**所有 Iota 类型直接实现 `Executable` 接口**，消除"纯数据"与"可执行"的二元对立。

## 核心设计理念

### 问题：DataWrapper 模式的缺陷

重构前的架构使用 `DataWrapper` 包装纯数据：
```
IData ← IExecutable ← Operation, ProgramBlock
  ↑
  └─ DataWrapper (包装 NumberIota 等)
```

这种设计的问题：
1. **概念冗余**：需要额外的包装类实现语法糖
2. **特例处理**：序列化/反序列化需要解包/包装逻辑
3. **语义不直观**：数据类型不能直接执行，需要包装

### 解决方案：统一 Executable 接口

重构后的架构：
```
Executable ← NumberIota, BooleanIota, StringIota, VectorIota, EntityIota, NullIota, Operation, ProgramBlock
```

**所有类型都实现 `Executable`，数据的执行语义是"将自己压入数据栈"**。

## 接口设计

```java
// engine 包 - 可执行数据接口
public interface Executable {
    /**
     * 获取数据类型标识符
     * @return 类型 ID，如 "relay:number"
     */
    String getType();

    /**
     * 获取原始值
     */
    Object getValue();

    /**
     * 序列化为 JSON
     */
    JsonElement toJson();

    /**
     * 执行此可执行单元
     * @param executor 状态机执行器
     */
    void execute(StateMachine executor);

    /**
     * 类型注册表接口
     */
    interface TypeRegistry {
        static void register(String typeId, JsonElementSerializer serializer, JsonElementDeserializer deserializer);
        static Executable fromJson(JsonElement json);
    }

    @FunctionalInterface
    interface JsonElementSerializer {
        JsonElement serialize(Executable data);
    }

    @FunctionalInterface
    interface JsonElementDeserializer {
        Executable deserialize(JsonObject json);
    }
}
```

## 类型实现模式

### 数据类型的执行语义

所有数据类型实现 `Executable`，`execute()` 方法将自己压入数据栈：

```java
// types 包 - 数字类型
public class NumberIota implements Executable {
    private final double value;

    public NumberIota(double value) {
        this.value = value;
    }

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

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);  // 数据执行时压栈
    }

    public double asDouble() { return value; }
    public int asInt() { return (int) value; }
}

// types 包 - 字符串类型
public class StringIota implements Executable {
    private final String value;

    public StringIota(String value) { this.value = value; }

    @Override
    public String getType() { return "relay:string"; }

    @Override
    public Object getValue() { return value; }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);  // 数据执行时压栈
    }

    public String asString() { return value; }
}

// types 包 - 空值类型（单例）
public class NullIota implements Executable {
    public static final NullIota INSTANCE = new NullIota();

    private NullIota() {}

    @Override
    public String getType() { return "relay:null"; }

    @Override
    public Object getValue() { return null; }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);  // 数据执行时压栈
    }
}
```

### 控制流类型的执行语义

```java
// types 包 - 操作引用
public class Operation implements Executable {
    private final String opId;

    public Operation(String opId) { this.opId = opId; }

    @Override
    public String getType() { return "relay:operation"; }

    @Override
    public Object getValue() { return opId; }

    @Override
    public void execute(StateMachine executor) {
        executor.executeOperation(opId);  // 调用操作注册表
    }
}

// types 包 - 程序块（列表）
public class ProgramBlock implements Executable {
    private final List<Executable> items;

    public ProgramBlock(List<Executable> items) {
        this.items = new ArrayList<>(items);
    }

    @Override
    public String getType() { return "relay:list"; }

    @Override
    public Object getValue() { return items; }

    @Override
    public void execute(StateMachine executor) {
        // 列表反转后压入程序栈，保证从左到右的执行顺序
        List<Executable> reversed = new ArrayList<>(items);
        Collections.reverse(reversed);
        for (Executable item : reversed) {
            executor.pushProgram(item);
        }
    }

    public List<Executable> getItems() { return new ArrayList<>(items); }
}
```

## 工厂类简化

```java
// types 包 - Iota 工厂类
public final class Iotas {
    private Iotas() {}

    // 直接返回类型实例，无需包装
    public static Executable number(double value) {
        return new NumberIota(value);
    }

    public static Executable number(int value) {
        return new NumberIota(value);
    }

    public static Executable booleanIota(boolean value) {
        return new BooleanIota(value);
    }

    public static Executable string(String value) {
        return new StringIota(value);
    }

    public static Executable vector(McVec3Adapter value) {
        return new VectorIota(value);
    }

    public static Executable entity(UUID value) {
        return new EntityIota(value);
    }

    public static Executable nullIota() {
        return NullIota.INSTANCE;
    }

    public static Executable list(List<Executable> items) {
        return new ProgramBlock(items);
    }

    public static Executable operation(String opId) {
        return new Operation(opId);
    }

    // 类型转换辅助
    public static NumberIota asNumber(Executable exec) {
        if (exec instanceof NumberIota n) return n;
        throw new IllegalArgumentException("期望 number 类型，实际为：" + exec.getType());
    }
    // ... 其他 asXxx() 方法
}
```

## StateMachine 双栈设计

```java
public class StateMachine {
    // 程序栈和数据栈都存储 Executable
    private final Deque<Executable> programStack = new ArrayDeque<>();
    private final Deque<Executable> dataStack = new ArrayDeque<>();

    // 加载程序
    public void loadProgram(List<Executable> program) {
        List<Executable> reversed = new ArrayList<>(program);
        Collections.reverse(reversed);
        programStack.clear();
        for (Executable iota : reversed) {
            programStack.push(iota);
        }
    }

    // Tick 执行
    public void tick(int ops) {
        remainingOps = ops;
        while (remainingOps > 0 && !programStack.isEmpty()) {
            Executable executable = programStack.pop();
            executable.execute(this);  // 每个类型自己决定如何执行
            remainingOps--;
        }
    }

    // 数据栈操作
    public Executable popData() {
        if (dataStack.isEmpty()) {
            triggerMishap("数据栈为空，无法弹出");
            return null;
        }
        return dataStack.pop();
    }

    public void pushData(Executable iota) {
        if (dataStack.size() >= maxStackSize) {
            triggerMishap("数据栈超出大小限制");
            return;
        }
        dataStack.push(iota);
    }

    // 调试快照
    public List<Executable> getDataStackSnapshot() {
        return new ArrayList<>(dataStack);
    }

    public List<Executable> getProgramStackSnapshot() {
        return new ArrayList<>(programStack);
    }
}
```

## 序列化器简化

```java
// mc 包 - NBT 序列化器
public class NbtSerializer {
    public static final NbtSerializer INSTANCE = new NbtSerializer();

    public CompoundTag serialize(Executable exec) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", exec.getType());

        // 直接类型判断，无需解包
        if (exec instanceof NumberIota num) {
            if (num.isInteger()) {
                tag.putInt("value", num.asInt());
            } else {
                tag.putDouble("value", num.asDouble());
            }
        } else if (exec instanceof BooleanIota bool) {
            tag.putBoolean("value", bool.asBoolean());
        } else if (exec instanceof StringIota str) {
            tag.putString("value", str.asString());
        } else if (exec instanceof VectorIota vec) {
            Vec3 v = vec.getVec3();
            tag.putDouble("x", v.x);
            tag.putDouble("y", v.y);
            tag.putDouble("z", v.z);
        } else if (exec instanceof EntityIota ent) {
            tag.putString("value", ent.asEntity().toString());
        } else if (exec instanceof ProgramBlock list) {
            tag.put("value", serializeList(list.getItems()));
        } else if (exec instanceof Operation op) {
            tag.putString("op", op.getOpId());
        } else if (exec instanceof NullIota) {
            // 无值
        }

        return tag;
    }

    public Executable deserialize(CompoundTag tag) {
        String typeName = tag.getString("type").orElse("unknown");

        return switch (typeName) {
            case "relay:number" -> {
                if (tag.contains("value")) {
                    var intOpt = tag.getInt("value");
                    if (intOpt.isPresent()) {
                        yield new NumberIota(intOpt.get());
                    } else {
                        yield new NumberIota(tag.getDouble("value").orElse(0.0));
                    }
                } else {
                    yield new NumberIota(0);
                }
            }
            case "relay:boolean" -> new BooleanIota(tag.getBoolean("value").orElse(false));
            case "relay:vector" -> new VectorIota(new Vec3(
                    tag.getDouble("x").orElse(0.0),
                    tag.getDouble("y").orElse(0.0),
                    tag.getDouble("z").orElse(0.0)));
            case "relay:string" -> new StringIota(tag.getString("value").orElse(""));
            case "relay:entity" -> {
                String uuidStr = tag.getString("value").orElse("");
                yield new EntityIota(uuidStr.isEmpty() ? new UUID(0, 0) : UUID.fromString(uuidStr));
            }
            case "relay:list" -> {
                ListTag listTag = tag.getList("value").orElse(new ListTag());
                yield new ProgramBlock(deserializeList(listTag));
            }
            case "relay:operation" -> new Operation(tag.getString("op").orElse(""));
            case "relay:null", "null", "any" -> NullIota.INSTANCE;
            default -> NullIota.INSTANCE;
        };
    }
}
```

## StateMachineNbtSerializer 简化

```java
// mc 包 - StateMachine NBT 序列化器
public class StateMachineNbtSerializer {
    public static final StateMachineNbtSerializer INSTANCE = new StateMachineNbtSerializer();
    private final NbtSerializer iotaSerializer = NbtSerializer.INSTANCE;

    public CompoundTag serialize(StateMachine machine) {
        CompoundTag tag = new CompoundTag();

        // 程序栈序列化
        ListTag programList = new ListTag();
        for (Executable iota : machine.getProgramStackSnapshot()) {
            programList.add(iotaSerializer.serialize(iota));
        }
        tag.put("programStack", programList);

        // 数据栈序列化（统一为 Executable）
        ListTag dataList = new ListTag();
        for (Executable data : machine.getDataStackSnapshot()) {
            dataList.add(iotaSerializer.serialize(data));
        }
        tag.put("dataStack", dataList);

        tag.putBoolean("hasWorldInteractor", machine.hasWorldInteractor());
        tag.putInt("maxStackSize", machine.getMaxStackSize());

        return tag;
    }

    public void deserialize(StateMachine machine, CompoundTag tag) {
        // 程序栈反序列化
        ListTag programList = tag.getList("programStack").orElse(new ListTag());
        List<Executable> programStack = new ArrayList<>();
        for (Tag element : programList) {
            programStack.add(iotaSerializer.deserialize((CompoundTag) element));
        }
        Collections.reverse(programStack);
        machine.loadProgram(programStack);

        // 数据栈反序列化
        ListTag dataList = tag.getList("dataStack").orElse(new ListTag());
        List<Executable> dataStack = new ArrayList<>();
        for (Tag element : dataList) {
            dataStack.add(iotaSerializer.deserialize((CompoundTag) element));
        }
        Collections.reverse(dataStack);
        for (Executable data : dataStack) {
            machine.pushData(data);
        }

        machine.setHasWorldInteractor(tag.getBoolean("hasWorldInteractor").orElse(false));
        machine.setMaxStackSize(tag.getInt("maxStackSize").orElse(1024));
    }
}
```

## 通信系统更新

```java
// core 包 - 通信系统
public class CommunicationSystem {
    // 频道队列存储 Executable
    private static final Map<Integer, Queue<Executable>> CHANNELS = new ConcurrentHashMap<>();
    public static final int MAX_QUEUE_SIZE = 1000;

    public static boolean send(int channel, Executable data) {
        Queue<Executable> queue = CHANNELS.computeIfAbsent(channel, k -> new ConcurrentLinkedQueue<>());
        if (queue.size() >= MAX_QUEUE_SIZE) return false;
        return queue.offer(data);
    }

    public static Executable recv(int channel) {
        Queue<Executable> queue = CHANNELS.get(channel);
        return (queue == null || queue.isEmpty()) ? NullIota.INSTANCE : queue.poll();
    }

    public static Executable peek(int channel) {
        Queue<Executable> queue = CHANNELS.get(channel);
        return (queue == null || queue.isEmpty()) ? NullIota.INSTANCE : queue.peek();
    }
}
```

## 设计优势对比

| 特性 | DataWrapper 模式 | 统一 Executable 模式 |
|------|-----------------|---------------------|
| 概念复杂度 | 需要理解 IData/IExecutable/DataWrapper 三层 | 所有类型都是 Executable |
| 代码量 | 需要 DataWrapper 类 + 包装逻辑 | 无需包装类 |
| 序列化 | 需要解包/包装逻辑 | 直接类型判断 |
| 语义直观性 | "数据不是操作，需要包装才能执行" | "所有类型都可执行，数据的执行是压栈" |
| 扩展性 | 新增类型需考虑是否实现 IData 或 IExecutable | 新增类型统一实现 Executable |

## 关键设计决策

| 决策 | 理由 |
|------|------|
| 所有类型实现 Executable | 统一接口，消除二元对立 |
| 数据类型 execute() 压栈 | 语义清晰：数据的执行就是将自己放入数据栈 |
| 双栈都存储 Executable | 类型统一，无需转换 |
| 移除 IData 接口 | 冗余抽象，Executable 已包含所有必要方法 |
| 工厂类直接返回类型实例 | 简化调用，无需包装 |

## 项目结构

```
src/main/java/qdream/relay/
├── engine/                    # 纯 Java 引擎（无 MC 依赖）
│   ├── Executable.java        # 统一接口（getType, getValue, toJson, execute）
│   ├── StateMachine.java      # 状态机执行器（双 Executable 栈）
│   ├── OperationRegistry.java # 操作注册表
│   ├── OperationSignature.java# 操作签名
│   └── IotaTypeRegistry.java  # 类型注册表
│
├── types/                     # 类型实现层
│   ├── NumberIota.java        # 实现 Executable，execute 时压栈
│   ├── BooleanIota.java       # 实现 Executable，execute 时压栈
│   ├── StringIota.java        # 实现 Executable，execute 时压栈
│   ├── VectorIota.java        # 实现 Executable，execute 时压栈
│   ├── EntityIota.java        # 实现 Executable，execute 时压栈
│   ├── NullIota.java          # 实现 Executable，execute 时压栈（单例）
│   ├── Operation.java         # 实现 Executable，execute 时调用操作
│   ├── ProgramBlock.java      # 实现 Executable，execute 时展开列表
│   ├── Iotas.java             # 工厂类（直接返回类型实例）
│   └── IotaSerializers.java   # 序列化器注册
│
├── mc/                        # MC 适配层
│   ├── McVec3Adapter.java     # Vec3 包装类
│   ├── NbtSerializer.java     # NBT 序列化（直接类型判断）
│   └── StateMachineNbtSerializer.java
│
├── core/                      # MC 集成层
│   ├── CommunicationSystem.java # 通信系统（Queue<Executable>）
│   ├── ShellContainer.java
│   └── ShellTickHandler.java
│
└── operations/                # 操作实现
    ├── base/                  # Push, Pop, Dup, Swap
    ├── arithmetic/            # Add, Sub, Mul, Div
    ├── logic/                 # And, Or, Not
    ├── control/               # If, Eval, Stop
    ├── communication/         # Send, Recv, Peek
    ├── list/                  # List 操作
    └── world/                 # 世界交互操作
```
