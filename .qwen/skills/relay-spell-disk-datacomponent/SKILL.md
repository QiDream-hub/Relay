---
name: relay-spell-disk-datacomponent
description: 使用 DataComponent 系统实现法术磁盘存储 - CompoundTag 包装 ListTag 模式
source: auto-skill
extracted_at: '2026-06-06T04:04:41.284Z'
---

# Minecraft 26.1.2 DataComponent 法术磁盘实现

## 问题背景
Minecraft 26.1.2 (1.21+) 使用 DataComponent 系统替代传统 NBT 存储物品数据。法术磁盘需要存储可执行的程序列表（`List<IExecutable>`），需要序列化为 NBT 格式。

## 核心实现模式

### 1. 数据结构设计

使用 `CompoundTag` 作为 DataComponent 类型，内部包装 `ListTag`：

```
SPELL_PROGRAM (CompoundTag)
  └── "program" (ListTag)
        ├── [0] (CompoundTag) - McIota #1
        ├── [1] (CompoundTag) - McIota #2
        └── ...
```

**为什么使用 CompoundTag 包装 ListTag？**
- `CompoundTag.CODEC` 已内置，无需自定义 Codec
- 便于未来扩展（可添加版本、元数据等字段）
- 避免直接暴露内部列表结构

### 2. 在物品类中注册 DataComponent

将注册逻辑放在物品类内部，避免额外的工具类：

```java
public class SpellDiskItem extends Item {
    public static final DataComponentType<CompoundTag> SPELL_PROGRAM = register("spell_program");

    private static DataComponentType<CompoundTag> register(String name) {
        ResourceKey<DataComponentType<?>> key = ResourceKey.create(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Relay.MOD_ID, name)
        );
        DataComponentType.Builder<CompoundTag> builder = DataComponentType.builder();
        
        // 持久化：使用 CompoundTag 内置 Codec
        builder.persistent(CompoundTag.CODEC);
        
        // 网络同步：使用 FriendlyByteBuf 读写 NBT
        StreamCodec<FriendlyByteBuf, CompoundTag> streamCodec = new StreamCodec<FriendlyByteBuf, CompoundTag>() {
            @Override
            public CompoundTag decode(FriendlyByteBuf buf) {
                try {
                    return buf.readNbt();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to decode CompoundTag", e);
                }
            }

            @Override
            public void encode(FriendlyByteBuf buf, CompoundTag tag) {
                try {
                    buf.writeNbt(tag);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to encode CompoundTag", e);
                }
            }
        };
        builder.networkSynchronized(streamCodec);
        
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, key, builder.build());
    }
}
```

### 3. 物品操作封装

```java
public class SpellDiskItem extends Item {
    private static final String TAG_PROGRAM = "program";

    public static List<IExecutable> getProgram(ItemStack stack) {
        CompoundTag programTag = stack.get(SPELL_PROGRAM);
        if (programTag == null) return List.of();
        
        Optional<ListTag> listOpt = programTag.getList(TAG_PROGRAM);
        if (listOpt.isEmpty()) return List.of();
        
        ListTag listTag = listOpt.get();
        List<IExecutable> result = new ArrayList<>();
        for (int i = 0; i < listTag.size(); i++) {
            Optional<CompoundTag> elementOpt = listTag.getCompound(i);
            elementOpt.ifPresent(tag -> result.add(NbtSerializer.deserializeStatic(tag)));
        }
        return result;
    }

    public static void setProgram(ItemStack stack, List<IExecutable> program) {
        CompoundTag programTag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (IExecutable iota : program) {
            if (iota instanceof McIota mcIota) {
                listTag.add(NbtSerializer.serializeStatic(mcIota));
            } else {
                throw new RuntimeException("不支持的 IExecutable 类型：" + iota.getClass());
            }
        }
        programTag.put(TAG_PROGRAM, listTag);
        stack.set(SPELL_PROGRAM, programTag);
    }

    public static void saveFromStateMachine(ItemStack stack, StateMachine machine) {
        List<IExecutable> program = new ArrayList<>(machine.getProgramStackSnapshot());
        Collections.reverse(program); // 恢复原始顺序
        setProgram(stack, program);
    }

    public static void loadToStateMachine(ItemStack stack, StateMachine machine) {
        List<IExecutable> program = getProgram(stack);
        if (!program.isEmpty()) {
            machine.loadProgram(program);
        }
    }

    public static boolean hasProgram(ItemStack stack) {
        return stack.has(SPELL_PROGRAM);
    }

    public static int getProgramSize(ItemStack stack) {
        CompoundTag programTag = stack.get(SPELL_PROGRAM);
        if (programTag == null) return 0;
        Optional<ListTag> listOpt = programTag.getList(TAG_PROGRAM);
        return listOpt.map(ListTag::size).orElse(0);
    }

    public static void clear(ItemStack stack) {
        stack.remove(SPELL_PROGRAM);
    }
}
```

### 4. 触发静态初始化

在主类 `onInitialize()` 中触发 DataComponent 注册：

```java
@Override
public void onInitialize() {
    // ... 其他注册 ...
    
    // 注册自定义 DataComponent
    SpellDiskItem.SPELL_PROGRAM.getClass(); // 触发静态初始化
    
    // ... 其他注册 ...
}
```

### 5. 序列化器适配

使用现有的 `NbtSerializer` 进行 `McIota` ↔ `CompoundTag` 转换：

```java
public class NbtSerializer {
    public static final NbtSerializer INSTANCE = new NbtSerializer();

    // 静态方法供外部调用
    public static CompoundTag serializeStatic(McIota iota) {
        return INSTANCE.serialize(iota);
    }

    public static McIota deserializeStatic(CompoundTag tag) {
        return INSTANCE.deserialize(tag);
    }

    // 序列化：按类型存储
    public CompoundTag serialize(McIota iota) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", iota.getType());
        switch (iota.getType()) {
            case "number" -> tag.putDouble("value", iota.asDouble());
            case "boolean" -> tag.putBoolean("value", iota.asBoolean());
            case "string" -> tag.putString("value", iota.asString());
            case "list" -> tag.put("value", serializeList(iota.asList()));
            // ... 其他类型 ...
        }
        return tag;
    }

    // 反序列化：按类型读取（26.1.2 getter 返回 Optional）
    public McIota deserialize(CompoundTag tag) {
        String typeName = tag.getString("type").orElse("unknown");
        return switch (typeName) {
            case "number" -> McIota.ofDouble(tag.getDouble("value").orElse(0.0));
            case "boolean" -> McIota.ofBoolean(tag.getBoolean("value").orElse(false));
            case "string" -> McIota.ofString(tag.getString("value").orElse(null));
            case "list" -> McIota.ofList(deserializeList(tag.getList("value").orElse(new ListTag())));
            // ... 其他类型 ...
            default -> McIota.ofNull();
        };
    }
}
```

## 关键注意事项

1. **CompoundTag getter 返回 Optional** (26.1.2 API 变更)：
   - `tag.getInt("key")` → `Optional<Integer>`
   - `tag.getList("key")` → `Optional<ListTag>`
   - `tag.getCompound("key")` → `Optional<CompoundTag>`
   - 必须使用 `.orElse(default)` 或 `.ifPresent()` 处理

2. **ListTag 迭代**：
   - 使用 `for (int i = 0; i < listTag.size(); i++)` 循环
   - `listTag.getCompound(i)` 返回 `Optional<CompoundTag>`

3. **网络同步必须添加**：如果物品需要在客户端显示（如容器插槽同步），**必须**提供 `networkSynchronized()` 的 `StreamCodec`。

4. **StreamCodec 实现要点**：
   - 使用 `FriendlyByteBuf.readNbt()` 读取 NBT
   - 使用 `FriendlyByteBuf.writeNbt()` 写入 NBT
   - 处理异常并包装为 `RuntimeException`

5. **注册时机**：通过访问静态字段触发类初始化（`SpellDiskItem.SPELL_PROGRAM.getClass()`）。

6. **类型安全**：反序列化时检查元素类型（`instanceof CompoundTag`）并使用 `Optional` 安全访问。

## 适用场景

- 物品需要存储复杂数据结构（列表、嵌套 NBT）
- 需要跨游戏会话持久化物品状态
- 需要与服务端同步物品数据（DataComponent 自动处理网络同步）
- **容器/插槽中的物品显示**：必须提供 `networkSynchronized` 的 StreamCodec

## 与传统 NBT 对比

| 特性 | 传统 NBT | DataComponent |
|------|----------|---------------|
| API | `stack.getNbt()` / `getTag()` | `stack.get(ComponentType)` |
| 类型安全 | 弱（手动解析） | 强（Codec 保证） |
| 网络同步 | 手动处理 | 自动（提供 streamCodec） |
| 扩展性 | 有限 | 支持自定义类型 |
| 持久化 + 网络 | 同一格式 | 分离（Codec + StreamCodec） |
| 空值处理 | `null` 检查 | `Optional` 或默认值 |
