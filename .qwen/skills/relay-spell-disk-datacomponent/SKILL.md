---
name: relay-spell-disk-datacomponent
description: 使用 DataComponent 系统实现物品存储 - 自定义 Codec 和网络同步 StreamCodec
source: auto-skill
extracted_at: '2026-06-06T00:00:00.000Z'
---

# Minecraft 26.1.2 DataComponent 物品存储实现

## 问题背景
Minecraft 26.1.2 (1.21+) 使用 DataComponent 系统替代传统 NBT 存储物品数据。法术磁盘需要存储可执行的程序列表（`List<IExecutable>`），需要序列化为 NBT 格式。

## 核心实现模式

### 1. 注册自定义 DataComponent 类型（带网络同步）

```java
public class RelayDataComponents {
    // NBT 持久化 Codec - 仅支持 NbtOps
    private static final Codec<ListTag> NBT_CODEC = new Codec<ListTag>() {
        @Override
        public <T> DataResult<Pair<ListTag, T>> decode(DynamicOps<T> ops, T input) {
            if (ops instanceof NbtOps) {
                if (input instanceof ListTag listTag) {
                    return DataResult.success(Pair.of(listTag, ops.empty()));
                }
                return DataResult.error(() -> "Expected ListTag, got " + input);
            }
            return DataResult.error(() -> "Unsupported ops type: " + ops);
        }

        @Override
        public <T> DataResult<T> encode(ListTag input, DynamicOps<T> ops, T prefix) {
            if (ops instanceof NbtOps) {
                return DataResult.success((T) input);
            }
            return DataResult.error(() -> "Unsupported ops type: " + ops);
        }
    };

    // 网络同步 StreamCodec - 使用 FriendlyByteBuf 直接读写 NBT
    private static final StreamCodec<ByteBuf, ListTag> STREAM_CODEC = new StreamCodec<ByteBuf, ListTag>() {
        @Override
        public ListTag decode(ByteBuf buf) {
            Tag tag = FriendlyByteBuf.readNbt(buf, NbtAccounter.unlimitedHeap());
            if (tag instanceof ListTag listTag) {
                return listTag;
            }
            return (tag == null) ? new ListTag() : throwException(tag.getClass());
        }

        @Override
        public void encode(ByteBuf buf, ListTag tag) {
            FriendlyByteBuf.writeNbt(buf, tag);
        }
    };

    public static final DataComponentType<ListTag> SPELL_PROGRAM = register(
            "spell_program",
            builder -> builder
                    .persistent(NBT_CODEC)
                    .networkSynchronized(STREAM_CODEC)  // 必须添加网络同步！
    );

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        ResourceKey<DataComponentType<?>> key = ResourceKey.create(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Relay.MOD_ID, name)
        );
        DataComponentType<T> type = builderOperator.apply(DataComponentType.builder()).build();
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, key, type);
    }
}
```

### 2. 物品操作封装

```java
public class SpellDiskItem extends Item {
    public static List<IExecutable> getProgram(ItemStack stack) {
        ListTag listTag = stack.get(SPELL_PROGRAM);
        if (listTag == null) return List.of();
        // 反序列化 ListTag -> List<IExecutable>
        return listTag.stream()
            .filter(t -> t instanceof CompoundTag)
            .map(t -> NbtSerializer.deserializeStatic((CompoundTag) t))
            .toList();
    }

    public static void setProgram(ItemStack stack, List<IExecutable> program) {
        ListTag listTag = new ListTag();
        for (IExecutable iota : program) {
            if (iota instanceof McIota mcIota) {
                listTag.add(NbtSerializer.serializeStatic(mcIota));
            }
        }
        stack.set(SPELL_PROGRAM, listTag);
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
}
```

### 3. 序列化器适配

为 DataComponent Codec 提供静态序列化方法：

```java
public class NbtSerializer {
    public static final NbtSerializer INSTANCE = new NbtSerializer();

    // 静态方法供 Codec 使用
    public static CompoundTag serializeStatic(McIota iota) {
        return INSTANCE.serialize(iota);
    }

    public static McIota deserializeStatic(CompoundTag tag) {
        return INSTANCE.deserialize(tag);
    }

    public CompoundTag serialize(McIota iota) { /* ... */ }
    public McIota deserialize(CompoundTag tag) { /* ... */ }
}
```

## 关键注意事项

1. **前向引用问题**: Codec 必须在 `register()` 调用之前定义，否则会出现"非法前向引用"编译错误

2. **网络同步必须添加**: 如果物品需要在客户端显示（如容器插槽同步），**必须**提供 `networkSynchronized()` 的 `StreamCodec`，否则会报错：
   ```
   Failed to encode packet 'clientbound/minecraft:container_set_slot'
   Only supports NbtOps
   ```

3. **StreamCodec 实现要点**:
   - 使用 `FriendlyByteBuf.readNbt(buf, NbtAccounter)` 读取 NBT
   - 使用 `FriendlyByteBuf.writeNbt(buf, tag)` 写入 NBT
   - 处理 `null` 和类型转换（`Tag` → `ListTag`）

4. **Codec 实现**: 26.1.2 的 DataComponent 要求提供 `Codec<T>`，对于 NBT 类型需要：
   - 检查 `DynamicOps` 是否为 `NbtOps`
   - 直接返回 NBT 对象（PASSTHROUGH 模式）

5. **注册时机**: 在主类 `onInitialize()` 中调用 `RelayDataComponents.register()`

6. **类型安全**: 反序列化时检查元素类型（`instanceof CompoundTag`）

## 适用场景

- 物品需要存储复杂数据结构（列表、嵌套 NBT）
- 需要跨游戏会话持久化物品状态
- 需要与服务端同步物品数据（DataComponent 自动处理网络同步）
- **容器/插槽中的物品显示**：必须提供 `networkSynchronized` 的 StreamCodec

## 与传统 NBT 对比

| 特性 | 传统 NBT | DataComponent |
|------|----------|---------------|
| API | `stack.getNbt()` | `stack.get(ComponentType)` |
| 类型安全 | 弱（手动解析） | 强（Codec 保证） |
| 网络同步 | 手动处理 | 自动（提供 streamCodec） |
| 扩展性 | 有限 | 支持自定义类型 |
| 持久化 + 网络 | 同一格式 | 分离（Codec + StreamCodec） |
