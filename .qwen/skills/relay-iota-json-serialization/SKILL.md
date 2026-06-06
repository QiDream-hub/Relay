---
name: relay-iota-json-serialization
description: Relay 模组中 IData/IExecutable 的 JSON 序列化/反序列化设计模式，支持可扩展的自定义类型
source: auto-skill
extracted_at: '2026-06-06T08:32:07.281Z'
---

# Relay Iota JSON 序列化设计

## 设计目标

1. **统一序列化接口** - 所有 `IExecutable` 实现都必须支持 JSON 序列化
2. **支持自定义类型** - 其他 mod 可注册自己的数据类型
3. **完全限定名避免冲突** - 使用 `modid:type` 格式
4. **类型与序列化分离** - 通过注册表管理序列化器

## 核心架构（2026-06-06 重构后）

### IData/IExecutable 接口定义

```java
// engine 包 - 纯 Java 接口
public interface IData {
    String getType();
    Object getValue();
    JsonElement toJson();

    // 类型注册表接口（嵌套在 IData 中）
    interface TypeRegistry {
        static void register(String typeId,
                            JsonElementSerializer serializer,
                            JsonElementDeserializer deserializer) {
            IotaTypeRegistry.register(typeId, serializer, deserializer);
        }

        static IData fromJson(JsonElement json) {
            return IotaTypeRegistry.fromJson(json);
        }
    }

    @FunctionalInterface
    interface JsonElementSerializer {
        JsonElement serialize(IData data);
    }

    @FunctionalInterface
    interface JsonElementDeserializer {
        IData deserialize(JsonObject json);
    }
}

public interface IExecutable extends IData {
    void execute(StateMachine executor);
    // 继承 getType(), getValue(), toJson()
}
```

### 类型注册表（engine 包）

```java
public class IotaTypeRegistry {
    public static class SerDePair {
        final IData.JsonElementSerializer serializer;
        final IData.JsonElementDeserializer deserializer;
        public SerDePair(...) { ... }
    }

    private static final Map<String, SerDePair> TYPES = new HashMap<>();

    public static void register(String typeId,
                               IData.JsonElementSerializer serializer,
                               IData.JsonElementDeserializer deserializer) {
        TYPES.put(typeId, new SerDePair(serializer, deserializer));
    }

    public static IData fromJson(JsonElement json) {
        JsonObject obj = json.getAsJsonObject();
        String typeId = obj.get("type").getAsString();
        SerDePair serDe = TYPES.get(typeId);
        if (serDe == null) {
            throw new RuntimeException("未知类型：" + typeId);
        }
        return serDe.deserializer.deserialize(obj);
    }

    public static JsonElement toJson(IData data) {
        String typeId = data.getType();
        SerDePair serDe = TYPES.get(typeId);
        if (serDe == null) {
            throw new RuntimeException("未注册类型：" + typeId);
        }
        return serDe.serializer.serialize(data);
    }
}
```

## 内置类型实现（IotaSerializers）

**2026-06-06 重构**：每个类型类自己实现 `toJson()`，`IotaSerializers` 只负责注册反序列化器。

```java
public class IotaSerializers {
    public static void register() {
        // 数字类型
        IData.TypeRegistry.register("relay:number",
            // 序列化 - 委托给 NumberIota.toJson()
            data -> ((NumberIota) data).toJson(),
            // 反序列化
            json -> {
                JsonElement valueElem = json.get("value");
                if (valueElem.isJsonPrimitive() && valueElem.getAsJsonPrimitive().isNumber()) {
                    // 检查是否真的是整数（没有小数部分）
                    if (valueElem.getAsString().contains(".")) {
                        return new NumberIota(valueElem.getAsDouble());
                    } else {
                        return new NumberIota(valueElem.getAsInt());
                    }
                }
                // 兼容旧格式：value 是字符串
                String valueStr = valueElem.getAsString();
                if (valueStr.contains(".")) {
                    return new NumberIota(Double.parseDouble(valueStr));
                } else {
                    return new NumberIota(Integer.parseInt(valueStr));
                }
            }
        );

        // 布尔类型
        IData.TypeRegistry.register("relay:boolean",
            data -> ((BooleanIota) data).toJson(),
            json -> new BooleanIota(json.get("value").getAsBoolean())
        );

        // 字符串类型
        IData.TypeRegistry.register("relay:string",
            data -> ((StringIota) data).toJson(),
            json -> new StringIota(json.get("value").getAsString())
        );

        // 操作类型（可执行单元）
        IData.TypeRegistry.register("relay:operation",
            data -> ((Operation) data).toJson(),
            json -> new Operation(json.get("op").getAsString())
        );

        // 列表类型（嵌套序列化）
        IData.TypeRegistry.register("relay:list",
            data -> ((ProgramBlock) data).toJson(),
            json -> {
                JsonArray array = json.get("value").getAsJsonArray();
                List<IExecutable> list = new ArrayList<>();
                for (JsonElement elem : array) {
                    list.add((IExecutable) IData.TypeRegistry.fromJson(elem));
                }
                return new ProgramBlock(list);
            }
        );

        // ... 其他类型：relay:vector, relay:entity, relay:null
    }
}
```

## 独立类型类实现

**2026-06-06 重构**：删除 `McIota`，每个类型使用独立类。

```java
// 数字类型
public class NumberIota implements IExecutable {
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
        if (value == (int) value) {
            json.addProperty("value", (int) value);
        } else {
            json.addProperty("value", value);
        }
        return json;
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);  // 数据压栈
    }

    // 类型转换
    public double asDouble() { return value; }
    public int asInt() { return (int) value; }
    public boolean isInteger() { return value == (int) value; }
}

// 字符串类型
public class StringIota implements IExecutable {
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

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);  // 数据压栈
    }

    public String asString() { return value; }
}

// 操作引用（可执行）
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

// 程序块（列表，可执行）
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

## 工厂类（Iotas）

```java
public final class Iotas {
    private Iotas() {}

    public static NumberIota number(double value) { return new NumberIota(value); }
    public static NumberIota number(int value) { return new NumberIota(value); }
    public static BooleanIota booleanIota(boolean value) { return new BooleanIota(value); }
    public static StringIota string(String value) { return new StringIota(value); }
    public static VectorIota vector(McVec3Adapter value) { return new VectorIota(value); }
    public static EntityIota entity(UUID value) { return new EntityIota(value); }
    public static NullIota nullIota() { return NullIota.INSTANCE; }
    public static ProgramBlock list(List<IExecutable> items) { return new ProgramBlock(items); }
    public static Operation operation(String opId) { return new Operation(opId); }

    // 类型转换辅助
    public static NumberIota asNumber(IData data) {
        if (data instanceof NumberIota n) return n;
        throw new IllegalArgumentException("期望 number 类型，实际为：" + data.getType());
    }
    // ... 其他 asXxx() 方法
}
```

## 命令解析集成

```java
private static List<IExecutable> parseProgram(String programStr) {
    List<IExecutable> program = new ArrayList<>();
    String[] instructions = programStr.split(";");

    for (String instr : instructions) {
        instr = instr.trim();
        if (instr.isEmpty()) continue;

        if (!instr.startsWith("{")) {
            throw new RuntimeException("无效的语法，必须使用 JSON 格式：" + instr);
        }

        try {
            JsonParser parser = new JsonParser();
            JsonElement jsonElem = parser.parse(instr);
            IExecutable iota = (IExecutable) IData.TypeRegistry.fromJson(jsonElem);
            program.add(iota);
        } catch (Exception e) {
            throw new RuntimeException("解析 JSON 失败：" + instr, e);
        }
    }

    return program;
}
```

## 初始化时机

在 `Relay.onInitialize()` 中调用：

```java
@Override
public void onInitialize() {
    LOGGER.info("Initializing Relay Mod");

    // 注册所有 Iota 类型
    McIotaTypes.register();

    // ... 其他注册 ...
}
```

## 扩展自定义类型

其他 mod 可以注册自己的类型：

```java
// 在 mod 初始化时
IData.TypeRegistry.register("mymod:custom_entity",
    // 序列化
    data -> {
        JsonObject json = new JsonObject();
        json.addProperty("type", "mymod:custom_entity");
        json.addProperty("entity_id", ((CustomEntity) data.getValue()).getId());
        return json;
    },
    // 反序列化
    json -> {
        UUID entityId = UUID.fromString(json.get("entity_id").getAsString());
        return new CustomEntityIota(entityId);
    }
);
```

## 关键设计决策

### 1. 为什么将 TypeRegistry 放在 IData 接口中？

- **自包含** - `IData` 定义了序列化需求，同时提供注册入口
- **无需额外导入** - 使用者只需知道 `IData.TypeRegistry`
- **函数式接口** - `JsonElementSerializer` 和 `JsonElementDeserializer` 可用 Lambda 表达式

### 2. 为什么使用注册表模式？

- **解耦** - `McIota` 不需要知道所有类型的实现细节
- **可扩展** - 第三方 mod 可以添加自己的类型
- **集中管理** - 所有序列化逻辑在一个地方注册

### 3. 为什么使用完全限定名？

- **避免命名冲突** - `mymod:number` 和 `relay:number` 可以共存
- **清晰的类型来源** - 从类型 ID 可以看出自哪个 mod
- **兼容 Minecraft 资源系统** - 与 `Identifier` 命名规范一致

### 4. 为什么所有 IData 都必须实现 toJson()？

- **统一性** - 所有数据都可以序列化为 JSON
- **可组合** - 列表类型可以递归序列化元素
- **持久化** - 可以方便地存储到 NBT/DataComponent

### 5. 操作注册使用完全限定名

操作注册表 (`OperationRegistry`) 使用完全限定名（如 `relay:add`），这样：
- **执行层纯净** - `engine` 包的 `StateMachine.executeOperation()` 直接使用完整 ID 查找
- **实现层负责** - `mc` 包的 `OperationsInit` 注册时添加 `relay:` 前缀
- **扩展友好** - 其他 mod 可以注册 `othermod:add`，不会冲突

```java
// 操作注册（OperationsInit.java）
OperationRegistry.register("relay:add", new AddOp())
    .requiresWorldInteractor(false)
    .register();

// 操作执行（McIota.execute）
else if (type == McIotaType.STRING) {
    // 字符串类型作为操作执行，直接使用完整 ID
    executor.executeOperation((String) value);
}
```

## 注意事项

1. **类型 ID 格式** - 必须使用 `modid:type` 格式，避免冲突
2. **初始化顺序** - `McIotaTypes.register()` 必须在解析程序前调用
3. **错误处理** - 未知类型抛出 `RuntimeException`，命令层应提供友好的错误消息
4. **嵌套类型** - 列表类型的元素递归调用 `fromJson()`，确保所有类型已注册
5. **数字精度** - JSON 不区分整数和浮点数，需要通过值的内容判断

## 使用示例

```java
// 创建程序
List<IExecutable> program = List.of(
    McIota.ofInt(5),
    McIota.ofInt(3),
    McIota.ofString("relay:add")  // 操作类型
);

// 序列化每个元素
for (IExecutable exec : program) {
    JsonElement json = exec.toJson();
    System.out.println(json.toString());
}
// 输出：
// {"type":"relay:number","value":5}
// {"type":"relay:number","value":3}
// {"type":"relay:operation","op":"relay:add"}

// 反序列化
String programStr = "{\"type\":\"relay:number\",\"value\":5};{\"type\":\"relay:number\",\"value\":3};{\"type\":\"relay:operation\",\"op\":\"relay:add\"}";
List<IExecutable> parsed = parseProgram(programStr);

// 运行程序
StateMachine machine = new StateMachine();
machine.loadProgram(parsed);
machine.tick(100);
```
