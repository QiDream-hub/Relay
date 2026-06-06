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
- [x] Iota 类型系统 (NUMBER, BOOLEAN, VECTOR, STRING, ENTITY, NULL)
- [x] 操作注册表 (OperationRegistry) - 位于 `mc` 包
- [x] 状态机执行器 (StateMachine) - 双栈模型
- [x] 通信系统 (CommunicationSystem) - 全局频道 Map
- [x] 基础物品 (运算核心、法术磁盘、能量模块)
- [x] 外壳方块和方块实体
- [x] 客户端编辑器基础 (Screen, Widget)

### 操作库
- [x] 基础操作：Pop, Dup, Swap (Push 由数据自身替代)
- [x] 算术操作：Add, Sub, Mul, Div
- [x] 逻辑操作：And, Or, Not
- [x] 比较操作：Eq, Lt, Gt
- [x] 控制流：Eval, If, Stop
- [x] 通信操作：Send, Recv, Peek
- [x] 列表操作：ListAppend, ListGet, ListSet, ListLength

### 已完成的 API 适配 (26.1.2)
- [x] 注册表 API：使用 `Registry.register(BuiltInRegistries.XXX, id, object)` 静态方法
- [x] Identifier：使用 `Identifier.fromNamespaceAndPath(namespace, path)`
- [x] CompoundTag：getter 方法返回 `Optional` 类型（getInt, getString, getBoolean 等）
- [x] BlockEntityType：使用 `FabricBlockEntityTypeBuilder.create().build()`
- [x] MenuType：构造函数需要 `(MenuSupplier, FeatureFlagSet)` 两个参数
- [x] AbstractWidget：使用 `extractWidgetRenderState(GuiGraphicsExtractor, ...)` 方法
- [x] 网络包：使用 `ByteBufCodecs.STRING_UTF8` 替代 `STRING`
- [x] NBT 序列化：基于对象自身的序列化系统（Data.toNbt/fromNbt）

### 待修复/注意事项
- [x] **缺失 ProgramBlock 类** - 已创建
- [x] **NumberIota 缺少 asInt() 方法** - 已添加
- [x] **StateMachine 缺少 hasWorldInteractor 相关方法** - 已添加
- [x] **CommunicationSystem 返回类型问题** - 已修复为 Executable
- [x] **导入路径错误** - OperationRegistry 已移至 mc 包，导入已更新
- [x] **Executable 缺少 getId() 方法** - 已添加
- [x] **所有操作类型转换错误** - 已从 `Spell` 改为 `Executable`
- [ ] **ItemStack 的 DataComponent 系统适配**（当前仍使用 NBT）
- [ ] **网络包注册 API**（PayloadTypeRegistry 在 26.1.2 中可能已变更）
- [ ] **BaseEntityBlock.codec() 需要实现正确的 MapCodec**
- [ ] **ShellBlockEntity 的 NBT 持久化需要改用 ValueInput/ValueOutput**
- [ ] **JSON 序列化器未实现** - ProgramBlock 和 ListIota 的 fromJson/toJson 方法为空

## 项目结构
```
Relay/
├── src/main/java/qdream/relay/
│   ├── blocks/           # 方块注册 (RelayBlocks, RelayBlockEntities)
│   ├── commands/         # 命令实现
│   ├── core/             # 核心系统 (CommunicationSystem, EnergySystem, Scheduler, Shell*)
│   ├── engine/           # 引擎核心 (Executable, StateMachine)
│   ├── entities/         # 实体类型
│   ├── items/            # 物品注册 (RelayItems, RelayDataComponents)
│   ├── mc/               # Minecraft 适配层 (OperationRegistry, StateMachineNbtSerializer)
│   │   └── base/         # 基类 (Operation, Spell, Data)
│   ├── mixin/            # Mixin 配置
│   ├── networking/       # 网络通信
│   ├── operations/       # 操作实现
│   │   ├── arithmetic/   # 算术操作
│   │   ├── base/         # 基础操作
│   │   ├── communication/# 通信操作
│   │   ├── control/      # 控制流
│   │   ├── list/         # 列表操作
│   │   └── logic/        # 逻辑操作
│   ├── screen/           # GUI 容器
│   ├── types/            # Iota 类型实现
│   └── Relay.java        # 主类
├── src/client/java/qdream/relay/client/
│   ├── editor/           # 法术编辑器
│   ├── screen/widget/    # UI Widget
│   └── RelayClient.java
└── docs/                 # 文档
```

## 构建命令
```bash
./gradlew build
./gradlew runClient
```

## 重要设计决策

### 1. 双栈执行模型
- **程序栈 (programStack)**: 存储所有可执行单元 (Executable)
- **数据栈 (dataStack)**: 存储临时值 (Executable)
- **执行流程**: tick() 从程序栈弹出并执行，数据由操作自行管理

### 2. Executable 统一接口
```java
public interface Executable {
    void execute(StateMachine executor);
}
```
- 所有 Iota 类型 (数据和操作) 都实现 Executable
- 数据执行时将自己压入数据栈
- 操作执行时查注册表并执行逻辑

### 3. 操作世界交互器检查
- 操作自己在 `execute()` 中检查世界交互器需求
- 无需 Spell 接口或 OperationEntry 元数据
- 需要世界交互器的操作开头检查:
  ```java
  if (!executor.hasWorldInteractor()) {
      executor.triggerMishap("操作需要世界交互器");
      return;
  }
  ```

### 4. 通信系统
- 全局 Map: `Map<Integer, Queue<Executable>> channels`
- 频道队列容量限制：1000
- 跨维度支持：所有维度共享同一全局 Map
- 基础操作：`send(channel, data)`, `recv(channel)`, `peek(channel)`

### 5. 外壳方块
- 4 个插槽：核心、磁盘、能量模块、世界交互器
- GUI 打开链路：BlockEntity 实现 MenuProvider，右键调用 player.openMenu()

## 当前架构问题 (已修复/待处理)

### 已修复的问题

#### 问题 1: 缺失 ProgramBlock
- **状态**: ✅ 已修复
- **解决方案**: 创建 `ProgramBlock` 类存储 `List<Executable>`
- **位置**: `types/ProgramBlock.java`

#### 问题 2: 类型转换错误
- **状态**: ✅ 已修复
- **原因**: `executor.popData()` 返回 `Executable`，代码期望 `Spell`
- **解决方案**: 所有操作改为使用 `Executable` 类型并进行 `instanceof` 检查

#### 问题 3: 缺失方法
- **状态**: ✅ 已修复
- **添加的方法**:
  - `Executable.getId()` - 获取唯一标识符
  - `NumberIota.asInt()` - 转换为整数
  - `NumberIota.getValue()` - 获取值
  - `NumberIota.isInteger()` - 检查是否为整数
  - `VectorIota.getVec3()` - 获取向量
  - `StateMachine.hasWorldInteractor()`, `setHasWorldInteractor()`

#### 问题 4: 导入路径错误
- **状态**: ✅ 已修复
- **变更**: `OperationRegistry` 从 `engine` 移至 `mc` 包

### 待处理的问题

#### 问题 5: JSON 序列化器未实现
- **状态**: ⚠️ 待实现
- **影响**: `ProgramBlock` 和 `ListIota` 的 `fromJson()`/`toJson()` 方法为空
- **解决方案**: 需要实现 `Executable.TypeRegistry` 或类似的类型注册表

#### 问题 6: DataComponent 系统适配
- **状态**: ⚠️ 待适配
- **影响**: ItemStack 仍使用 NBT 存储数据

### 问题 4: 缺失方法
- **StateMachine**: 缺少 `hasWorldInteractor()`, `setHasWorldInteractor()`
- **NumberIota**: 缺少 `asInt()` 方法
- **影响**: 编译失败，序列化/反序列化无法工作

### 问题 5: 导入路径错误
- **现状**: OperationRegistry 从 `engine` 移至 `mc` 包，导入未更新
- **位置**: `SpellEditorScreenHandler.java`, `RelayServerNetworking.java`
- **影响**: 编译失败
