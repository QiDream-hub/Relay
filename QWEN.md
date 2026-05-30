# Relay Mod 项目信息

## 项目概述
Relay - 基于栈的编程模组，为 Minecraft 添加可视化法术编程系统

## 版本信息
- **Minecraft 版本**: 26.1.2 (1.21+)
- **Fabric Loader**: 0.19.2
- **Fabric Loom**: 1.16-SNAPSHOT
- **Fabric API**: 0.150.0+26.1.2
- **Java 版本**: 25

## 26.1.2 API 重大变更

### 1. 包路径变更
| 旧路径 | 新路径 |
|--------|--------|
| `net.minecraft.item.*` | `net.minecraft.world.item.*` |
| `net.minecraft.block.*` | `net.minecraft.world.level.block.*` |
| `net.minecraft.screen.*` | `net.minecraft.world.inventory.*` |
| `net.minecraft.text.Text` | `net.minecraft.network.chat.Component` |
| `net.minecraft.util.Identifier` | `net.minecraft.resources.Identifier` |

### 2. NBT 类重命名
| 旧类名 | 新类名 |
|--------|--------|
| `NbtCompound` | `CompoundTag` |
| `NbtList` | `ListTag` |
| `NbtElement` | `Tag` |

### 3. 注册表 API 变更
```java
// 旧方式
Identifier id = Identifier.of(MOD_ID, path);
Registry.register(Registries.ITEM, id, item);

// 新方式 (26.1.2)
Identifier id = Identifier.tryBySeparator(':', MOD_ID, path).orElseThrow();
BuiltInRegistries.ITEM.register(id, item);
```

### 4. ItemStack 使用 DataComponent 系统
26.1.2 版本使用 DataComponent 替代传统 NBT：
- `stack.getNbt()` → `stack.getComponents()`
- `stack.getOrCreateNbt()` → `stack.set(ComponentType, value)`

### 5. CompoundTag 方法变更
- `tag.getUuid()` → `tag.getUUID()` (返回 Optional<UUID>)
- `tag.getInt()` → 返回 `Optional<Integer>`
- `tag.getBoolean()` → 返回 `Optional<Boolean>`
- `tag.getDouble()` → 返回 `Optional<Double>`
- `tag.getString()` → 返回 `Optional<String>`
- `tag.getList(key, type)` → 第二个参数使用 byte 值 (Tag.TAG_COMPOUND = 10)
- `tag.contains(key, type)` → 第二个参数使用 byte 值 (Tag.DOUBLE = 6)

### 6. 方块属性 API
- `DirectionProperty` 类可能已移除或重命名
- `Properties.HORIZONTAL_FACING` 可能需要使用新的 API

### 7. 网络包 API
```java
// 旧方式
public static final Id<Payload> PACKET_ID = new Id<>(Identifier.of(MOD_ID, "packet"));
public static final PacketCodec<RegistryByteBuf, Payload> CODEC = ...;

// 新方式 (26.1.2)
public static final Type<Payload> TYPE = new Type<>(Identifier.tryBySeparator(':', MOD_ID, "packet").orElseThrow());
public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = ...;
```

### 8. Client 类变更
- `HandledScreen` → `AbstractContainerScreen`
- `ScreenHandler` → `AbstractContainerMenu`
- `PlayerInventory` → `Inventory`
- `ButtonWidget` → `Button`
- `ClickableWidget` → `AbstractWidget`

## 当前实现状态

### 已完成的核心功能
- [x] Iota 类型系统 (NUMBER, BOOLEAN, VECTOR, STRING, ENTITY, LIST, NULL)
- [x] 操作注册表 (OperationRegistry)
- [x] 状态机执行器 (StateMachine) - 双栈模型
- [x] 通信系统 (CommunicationSystem)
- [x] 基础物品 (运算核心、法术磁盘、能量模块)
- [x] 外壳方块和方块实体
- [x] 客户端编辑器基础 (Screen, Widget)

### 操作库
- [x] 基础操作：Push, Pop, Dup, Swap
- [x] 算术操作：Add, Sub, Mul, Div
- [x] 逻辑操作：And, Or, Not
- [x] 比较操作：Eq, Lt, Gt
- [x] 控制流：Eval, If
- [x] 通信操作：Send, Recv, Peek

### 已完成的 API 适配 (26.1.2)
- [x] 注册表 API：使用 `Registry.register(BuiltInRegistries.XXX, id, object)` 静态方法
- [x] Identifier：使用 `Identifier.fromNamespaceAndPath(namespace, path)`
- [x] CompoundTag：getter 方法返回 `Optional` 类型（getInt, getString, getBoolean 等）
- [x] BlockEntityType：使用 `FabricBlockEntityTypeBuilder.create().build()`
- [x] MenuType：构造函数需要 `(MenuSupplier, FeatureFlagSet)` 两个参数
- [x] AbstractWidget：使用 `extractWidgetRenderState(GuiGraphicsExtractor, ...)` 方法
- [x] 网络包：使用 `ByteBufCodecs.STRING_UTF8` 替代 `STRING`

### 待修复/注意事项
- [ ] ItemStack 的 DataComponent 系统适配（当前仍使用 NBT）
- [ ] 网络包注册 API（PayloadTypeRegistry 在 26.1.2 中可能已变更）
- [ ] BaseEntityBlock.codec() 需要实现正确的 MapCodec
- [ ] ShellBlockEntity 的 NBT 持久化需要改用 ValueInput/ValueOutput

## 项目结构
```
Relay/
├── src/main/java/qdream/relay/
│   ├── core/           # 核心系统 (Iota, StateMachine, OperationRegistry)
│   ├── items/          # 物品注册
│   ├── blocks/         # 方块注册
│   ├── operations/     # 操作实现
│   ├── networking/     # 网络通信
│   └── Relay.java      # 主类
├── src/client/java/qdream/relay/client/
│   ├── editor/         # 法术编辑器
│   ├── screen/widget/  # UI Widget
│   └── RelayClient.java
└── docs/               # 文档
```

## 构建命令
```bash
./gradlew build
./gradlew runClient
```

## 重要设计决策
1. **双栈执行模型**: 程序栈存可执行单元，数据栈存临时值
2. **操作注册链式 API**: `register(id).signature().cost().requiresWorldInteractor().register()`
3. **通信系统**: 全局 Map，频道队列容量限制 1000
4. **外壳方块**: 4 个插槽 (核心、磁盘、能量模块、世界交互器)
