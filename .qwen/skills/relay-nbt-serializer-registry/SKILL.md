---
name: relay-nbt-serializer-serializer-registry
description: Relay 模组中基于注册表的 NBT 序列化/反序列化架构，支持可扩展的自定义数据类型
source: auto-skill
extracted_at: '2026-06-07T00:00:00.000Z'
---

# Relay NBT 序列化器注册表架构

## 设计目标

1. **可扩展性** - 其他模组可以注册自定义数据类型的序列化器
2. **模块化** - 每个类型的序列化逻辑独立，不集中在单一类中
3. **类型安全** - 通过注册表确保序列化和反序列化使用相同的逻辑
4. **向后兼容** - 保留旧的 API 作为包装器，但推荐使用新 API

## 核心架构（2026-06-07 重构后）

### OperationRegistry 扩展

`OperationRegistry` 现在管理三类注册：
1. **操作实例注册表** (`OPERATIONS`) - 存储单例操作实例，用于执行
2. **数据类型工厂注册表** (`DATA_FACTORIES`) - 存储工厂方法，用于创建数据实例
3. **NBT 序列化器注册表** (`NBT_SERIALIZERS`) - 存储序列化/反序列化器

```java
public class OperationRegistry {
    // 操作实例注册表（用于执行）
    private static final Map<String, Executable> OPERATIONS = new HashMap<>();

    // 数据类型工厂注册表（用于反序列化）
    private static final Map<String, DataFactory> DATA_FACTORIES = new HashMap<>();

    // NBT 序列化器注册表
    private static final Map<String, NbtSerializer> NBT_SERIALIZERS = new HashMap<>();

    // 注册操作（单例）
    public static void register(String id, Executable operation);

    // 注册数据类型
    public static void registerData(String id, DataFactory factory);

    // 注册 NBT 序列化器
    public static void registerNbtSerializer(String id, NbtSerializer serializer);

    // 通过注册表序列化
    public static Optional<CompoundTag> serializeToNbt(Executable exec);

    // 通过注册表反序列化
    public static Optional<Executable> deserializeFromNbt(CompoundTag tag);

    // 检查注册
    public static boolean contains(String id);
    public static boolean containsData(String id);
    public static boolean containsNbtSerializer(String id);

    // 获取所有 ID
    public static Set<String> getAllOperationIds();
    public static Set<String> getAllDataIds();
    public static Set<String> getAllNbtSerializerIds();

    // 内部接口
    @FunctionalInterface
    public interface DataFactory {
        Data create();
    }

    interface NbtSerializer {
        void toNbt(Executable exec, CompoundTag tag);
        Executable fromNbt(CompoundTag tag);
    }
}
```

### NbtSerializers 注册工具类

创建 `NbtSerializers` 类集中注册所有内置数据类型的序列化器：

```java
public class NbtSerializers {
    private NbtSerializers() {}

    public static void register() {
        registerNumberSerializer();
        registerBooleanSerializer();
        registerStringSerializer();
        registerVectorSerializer();
        registerEntitySerializer();
        registerNullSerializer();
        registerListSerializer();
    }

    private static void registerNumberSerializer() {
        OperationRegistry.registerNbtSerializer("relay:number", new OperationRegistry.NbtSerializer() {
            @Override
            public void toNbt(Executable exec, CompoundTag tag) {
                NumberIota num = (NumberIota) exec;
                if (num.isInteger()) {
                    tag.putInt("value", num.asInt());
                } else {
                    tag.putDouble("value", num.asDouble());
                }
            }

            @Override
            public Executable fromNbt(CompoundTag tag) {
                var intOpt = tag.getInt("value");
                if (intOpt.isPresent()) {
                    return new NumberIota(intOpt.get());
                } else {
                    return new NumberIota(tag.getDouble("value").orElse(0.0));
                }
            }
        });
    }

    private static void registerBooleanSerializer() {
        OperationRegistry.registerNbtSerializer("relay:boolean", new OperationRegistry.NbtSerializer() {
            @Override
            public void toNbt(Executable exec, CompoundTag tag) {
                BooleanIota bool = (BooleanIota) exec;
                tag.putBoolean("value", bool.asBoolean());
            }

            @Override
            public Executable fromNbt(CompoundTag tag) {
                return new BooleanIota(tag.getBoolean("value").orElse(false));
            }
        });
    }

    private static void registerStringSerializer() {
        OperationRegistry.registerNbtSerializer("relay:string", new OperationRegistry.NbtSerializer() {
            @Override
            public void toNbt(Executable exec, CompoundTag tag) {
                StringIota str = (StringIota) exec;
                tag.putString("value", str.asString());
            }

            @Override
            public Executable fromNbt(CompoundTag tag) {
                return new StringIota(tag.getString("value").orElse(""));
            }
        });
    }

    private static void registerVectorSerializer() {
        OperationRegistry.registerNbtSerializer("relay:vector", new OperationRegistry.NbtSerializer() {
            @Override
            public void toNbt(Executable exec, CompoundTag tag) {
                VectorIota vec = (VectorIota) exec;
                Vec3 v = vec.getVec3();
                tag.putDouble("x", v.x);
                tag.putDouble("y", v.y);
                tag.putDouble("z", v.z);
            }

            @Override
            public Executable fromNbt(CompoundTag tag) {
                return new VectorIota(new Vec3(
                    tag.getDouble("x").orElse(0.0),
                    tag.getDouble("y").orElse(0.0),
                    tag.getDouble("z").orElse(0.0)
                ));
            }
        });
    }

    private static void registerEntitySerializer() {
        OperationRegistry.registerNbtSerializer("relay:entity", new OperationRegistry.NbtSerializer() {
            @Override
            public void toNbt(Executable exec, CompoundTag tag) {
                EntityIota ent = (EntityIota) exec;
                tag.putString("value", ent.asEntity().toString());
            }

            @Override
            public Executable fromNbt(CompoundTag tag) {
                String uuidStr = tag.getString("value").orElse("");
                UUID uuid = uuidStr.isEmpty() ? new UUID(0, 0) : UUID.fromString(uuidStr);
                return new EntityIota(uuid);
            }
        });
    }

    private static void registerNullSerializer() {
        OperationRegistry.registerNbtSerializer("relay:null", new OperationRegistry.NbtSerializer() {
            @Override
            public void toNbt(Executable exec, CompoundTag tag) {
                // NullIota 没有数据需要序列化
            }

            @Override
            public Executable fromNbt(CompoundTag tag) {
                return NullIota.INSTANCE;
            }
        });
    }

    private static void registerListSerializer() {
        OperationRegistry.registerNbtSerializer("relay:list", new OperationRegistry.NbtSerializer() {
            @Override
            public void toNbt(Executable exec, CompoundTag tag) {
                ListIota list = (ListIota) exec;
                ListTag listTag = serializeList(list.getValue());
                tag.put("value", listTag);
            }

            @Override
            public Executable fromNbt(CompoundTag tag) {
                var valueOpt = tag.getList("value");
                if (valueOpt.isPresent()) {
                    ListTag listTag = valueOpt.get();
                    return new ListIota(deserializeList(listTag));
                } else {
                    return NullIota.INSTANCE;
                }
            }

            private ListTag serializeList(List<Executable> list) {
                ListTag listTag = new ListTag();
                for (Executable item : list) {
                    var itemTagOpt = OperationRegistry.serializeToNbt(item);
                    itemTagOpt.ifPresent(listTag::add);
                }
                return listTag;
            }

            private List<Executable> deserializeList(ListTag listTag) {
                List<Executable> list = new ArrayList<>();
                for (Tag element : listTag) {
                    if (element instanceof CompoundTag compoundTag) {
                        var itemOpt = OperationRegistry.deserializeFromNbt(compoundTag);
                        itemOpt.ifPresent(list::add);
                    }
                }
                return list;
            }
        });
    }
}
```

### 初始化链路

在 `RelayOperations.register()` 中调用序列化器注册：

```java
public class RelayOperations {
    public static void register() {
        // 1. 注册数据类型
        registerDataTypes();

        // 2. 注册操作
        registerOperations();

        // 3. 注册 NBT 序列化器
        NbtSerializers.register();
    }

    private static void registerDataTypes() {
        OperationRegistry.registerData("relay:number", () -> new NumberIota(0));
        OperationRegistry.registerData("relay:boolean", () -> new BooleanIota(false));
        OperationRegistry.registerData("relay:string", () -> new StringIota(""));
        OperationRegistry.registerData("relay:vector", () -> new VectorIota(new Vec3(0, 0, 0)));
        OperationRegistry.registerData("relay:entity", () -> new EntityIota(new UUID(0, 0)));
        OperationRegistry.registerData("relay:null", NullIota::new);
        OperationRegistry.registerData("relay:list", () -> new ListIota(new ArrayList<>()));
    }

    private static void registerOperations() {
        // 注册 21 个操作...
    }
}
```

### 旧 API 废弃包装

旧的 `NbtSerializer` 类标记为 `@Deprecated`，内部实现委托给新的注册表系统：

```java
@Deprecated
public class NbtSerializer {
    public static final NbtSerializer INSTANCE = new NbtSerializer();

    private NbtSerializer() {}

    @Deprecated
    public static CompoundTag serializeStatic(Executable exec) {
        return OperationRegistry.serializeToNbt(exec).orElse(new CompoundTag());
    }

    @Deprecated
    public static Executable deserializeStatic(CompoundTag tag) {
        return OperationRegistry.deserializeFromNbt(tag).orElse(null);
    }

    @Deprecated
    public CompoundTag serialize(Executable exec) {
        return OperationRegistry.serializeToNbt(exec).orElse(new CompoundTag());
    }

    @Deprecated
    public Executable deserialize(CompoundTag tag) {
        return OperationRegistry.deserializeFromNbt(tag).orElse(null);
    }

    @Deprecated
    public ListTag serializeList(List<Executable> list) {
        ListTag listTag = new ListTag();
        for (Executable exec : list) {
            OperationRegistry.serializeToNbt(exec).ifPresent(listTag::add);
        }
        return listTag;
    }

    @Deprecated
    public List<Executable> deserializeList(ListTag listTag) {
        List<Executable> list = new ArrayList<>();
        for (Tag element : listTag) {
            if (element instanceof CompoundTag compoundTag) {
                OperationRegistry.deserializeFromNbt(compoundTag).ifPresent(list::add);
            }
        }
        return list;
    }
}
```

## 使用方式

### 序列化 Executable

```java
import qdream.relay.mc.OperationRegistry;
import qdream.relay.types.NumberIota;
import net.minecraft.nbt.CompoundTag;

// 创建数据类型实例
NumberIota number = new NumberIota(42);

// 通过注册表序列化
Optional<CompoundTag> tagOpt = OperationRegistry.serializeToNbt(number);
tagOpt.ifPresent(tag -> {
    // tag 包含：{id: "relay:number", value: 42}
    compoundTag.put("myData", tag);
});
```

### 反序列化 Executable

```java
import qdream.relay.mc.OperationRegistry;
import qdream.relay.engine.Executable;

// 从 NBT 读取
CompoundTag tag = compoundTag.getCompound("myData");
Optional<Executable> execOpt = OperationRegistry.deserializeFromNbt(tag);
execOpt.ifPresent(exec -> {
    // exec 是反序列化后的 Executable 实例
    stateMachine.pushData(exec);
});
```

### 序列化列表（递归）

```java
import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationRegistry;
import net.minecraft.nbt.ListTag;
import java.util.List;

List<Executable> program = getProgram();
ListTag listTag = new ListTag();

// 递归序列化每个元素
for (Executable exec : program) {
    OperationRegistry.serializeToNbt(exec).ifPresent(listTag::add);
}
compoundTag.put("program", listTag);
```

### 反序列化列表（递归）

```java
import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationRegistry;
import net.minecraft.nbt.ListTag;
import java.util.ArrayList;
import java.util.List;

ListTag listTag = compoundTag.getList("program").orElse(new ListTag());
List<Executable> program = new ArrayList<>();

for (Tag element : listTag) {
    if (element instanceof CompoundTag tag) {
        OperationRegistry.deserializeFromNbt(tag).ifPresent(program::add);
    }
}
```

## 注册自定义数据类型序列化器

其他模组可以注册自己的数据类型序列化器：

```java
import qdream.relay.mc.OperationRegistry;
import qdream.relay.engine.Executable;
import net.minecraft.nbt.CompoundTag;

// 注册数据类型工厂
OperationRegistry.registerData("mymod:customType", () -> new CustomData());

// 注册 NBT 序列化器
OperationRegistry.registerNbtSerializer("mymod:customType", new OperationRegistry.NbtSerializer() {
    @Override
    public void toNbt(Executable exec, CompoundTag tag) {
        CustomData data = (CustomData) exec;
        tag.putString("customValue", data.getCustomValue());
    }

    @Override
    public Executable fromNbt(CompoundTag tag) {
        String value = tag.getString("customValue").orElse("");
        return new CustomData(value);
    }
});
```

## 已注册的序列化器

| 类型 ID | 说明 | NBT 字段 |
|---------|------|----------|
| `relay:number` | 数字类型 | `value` (int 或 double) |
| `relay:boolean` | 布尔类型 | `value` (boolean) |
| `relay:string` | 字符串类型 | `value` (string) |
| `relay:vector` | 向量类型 | `x`, `y`, `z` (double) |
| `relay:entity` | 实体类型 | `value` (UUID string) |
| `relay:null` | 空值类型 | 无字段 |
| `relay:list` | 列表类型 | `value` (ListTag，递归序列化) |

## Minecraft 26.1.2 API 适配

在 26.1.2 版本中，`CompoundTag` 的 getter 方法返回 `Optional` 类型：

```java
// 26.1.2 API
Optional<Integer> intOpt = tag.getInt("value");
Optional<Double> doubleOpt = tag.getDouble("value");
Optional<String> stringOpt = tag.getString("value");
Optional<Boolean> booleanOpt = tag.getBoolean("value");
Optional<ListTag> listOpt = tag.getList("value");

// 使用方式
if (intOpt.isPresent()) {
    int value = intOpt.get();
}

// 或者使用 orElse
int value = tag.getInt("value").orElse(0);
```

### 注意事项

1. **`getList` 方法签名变更** - 26.1.2 中 `getList(String key)` 返回 `Optional<ListTag>`，不再需要第二个参数
2. **`contains` 方法签名变更** - 26.1.2 中 `contains(String key)` 只需要一个参数，不再需要类型检查
3. **使用 `Optional` 处理** - 所有 getter 都必须处理 `Optional`，使用 `isPresent()` 或 `orElse()`

## 关键设计决策

### 1. 为什么使用注册表模式？

- **解耦** - 序列化逻辑从硬编码的 `instanceof` 判断解耦到注册表
- **可扩展** - 其他模组可以注册自己的数据类型序列化器
- **模块化** - 每个类型的序列化逻辑独立，易于维护
- **可测试性** - 可以轻松替换或模拟序列化器进行测试

### 2. 为什么序列化器接口放在 OperationRegistry 内部？

- **包私有** - `NbtSerializer` 接口不需要公开，只在 `OperationRegistry` 内部使用
- **减少命名空间污染** - 避免创建额外的公开接口
- **集中管理** - 序列化器与注册表紧密耦合，放在一起更清晰

### 3. 为什么列表类型递归调用注册表？

```java
private ListTag serializeList(List<Executable> list) {
    ListTag listTag = new ListTag();
    for (Executable item : list) {
        var itemTagOpt = OperationRegistry.serializeToNbt(item);  // 递归调用
        itemTagOpt.ifPresent(listTag::add);
    }
    return listTag;
}
```

- **支持嵌套类型** - 列表可以包含任意类型的元素，包括其他列表
- **统一序列化逻辑** - 所有类型都通过注册表序列化，保证一致性
- **支持自定义类型** - 其他模组的类型也可以正确序列化

### 4. 为什么保留旧的 NbtSerializer 类？

- **向后兼容** - 现有代码可以继续使用旧 API，无需立即迁移
- **渐进式迁移** - 可以逐步将代码迁移到新 API
- **包装器开销小** - 旧 API 只是简单委托给新 API，性能开销可忽略

## 与 JSON 序列化系统的关系

Relay 模组有两套序列化系统：

| 特性 | JSON 序列化 | NBT 序列化 |
|------|------------|-----------|
| 用途 | 法术程序字符串解析（命令/GUI） | 物品/方块数据持久化 |
| 注册表 | `IotaTypeRegistry` | `OperationRegistry` |
| 格式 | JSON 文本 | Minecraft NBT |
| 扩展方式 | `IData.TypeRegistry.register()` | `OperationRegistry.registerNbtSerializer()` |

两套系统设计相似，都使用注册表模式，但服务于不同的使用场景。

## 注意事项

1. **类型 ID 格式** - 必须使用 `modid:type` 格式，避免冲突
2. **初始化顺序** - `NbtSerializers.register()` 必须在序列化/反序列化前调用
3. **错误处理** - 未知类型返回 `Optional.empty()`，调用者应处理
4. **嵌套类型** - 列表类型的元素递归调用注册表，确保所有类型已注册
5. **Optional 处理** - 26.1.2 API 中所有 getter 返回 `Optional`，必须正确处理
