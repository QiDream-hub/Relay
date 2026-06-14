---
name: relay-editor-sync
description: Relay 模组法术编辑器服务端主动同步模式 - 打开编辑器时自动同步程序列表到客户端
source: auto-skill
extracted_at: '2026-06-14T09:03:56.350Z'
---

# Relay 模组法术编辑器服务端主动同步模式

## 核心问题

在 Minecraft 26.1.2 中，使用 `AbstractContainerMenu` 实现的 GUI 存在**客户端/服务端状态不同步**问题：

- 服务端的 `program` 列表从磁盘加载
- 客户端 `ProgramListWidget` 通过 `Supplier` 访问服务端状态
- 但 Minecraft 的 `AbstractContainerMenu` **不会自动同步自定义字段**
- 打开编辑器时，客户端渲染的是空列表（服务端还没完成加载）

**根本原因**：
```
玩家右键方块 → 服务端创建 ScreenHandler → 加载程序到服务端 program 字段
                          ↓
              客户端创建 Screen → Widget 使用 Supplier 访问服务端状态
                          ↓
              但服务端 program 字段初始为空，且不会自动同步
```

## 解决方案：服务端主动同步

采用**服务端主动同步**模式，在打开编辑器时发送 S2C 包将程序列表同步到客户端。

### 完整同步链路

```
玩家右键方块
  → SpellEditorBlockEntity.createMenu()
  → 检测磁盘并调用 loadProgramFromDisk()
  → syncProgramToClient() 序列化程序为 NBT
  → 发送 S2C_SyncSpellDiskPayload
  → 客户端接收包并解析 NBT
  → 调用 SpellEditorScreen.updateProgramFromServer()
  → ProgramListWidget 刷新显示
```

## 实现步骤

### 1. SpellEditorScreenHandler 添加同步方法

```java
public class SpellEditorScreenHandler extends AbstractContainerMenu {
    private final List<Executable> program;
    private final SpellEditorBlockEntity blockEntity;

    /**
     * 从磁盘加载程序并同步到客户端
     */
    public void loadProgramFromDisk(ItemStack diskStack) {
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof SpellDiskItem)) {
            return;
        }

        List<Executable> loadedProgram = SpellDiskItem.getProgram(diskStack);
        this.program.clear();
        this.program.addAll(loadedProgram);
        
        // 同步到客户端
        syncProgramToClient();
    }

    /**
     * 将当前程序列表同步到客户端
     */
    public void syncProgramToClient() {
        if (blockEntity != null && blockEntity.getLevel() != null) {
            net.minecraft.world.level.Level level = blockEntity.getLevel();
            // 检查是否在服务端
            if (!level.isClientSide()) {
                // 获取打开此编辑器的玩家
                level.players().stream()
                    .filter(p -> p.containerMenu == this)
                    .filter(p -> p instanceof ServerPlayer)
                    .findFirst()
                    .ifPresent(p -> {
                        try {
                            ListTag programList = ProgramCompiler.toNbt(this.program);
                            CompoundTag programTag = new CompoundTag();
                            programTag.put("program", programList);
                            ServerPlayNetworking.send(
                                (ServerPlayer) p,
                                new S2C_SyncSpellDiskPayload(programTag)
                            );
                        } catch (ProgramCompiler.CompilationException e) {
                            e.printStackTrace();
                        }
                    });
            }
        }
    }

    /**
     * 从服务端更新程序列表（仅供客户端同步使用）
     */
    public void setProgramFromServer(List<Executable> program) {
        this.program.clear();
        this.program.addAll(program);
    }
}
```

**关键点**：
- `isClientSide()` 检查确保只在服务端执行同步
- 通过 `players().stream()` 找到打开此编辑器的玩家
- 序列化程序为 NBT 并通过 `ServerPlayNetworking.send()` 发送

### 2. SpellEditorBlockEntity 在 createMenu() 中触发加载

```java
public class SpellEditorBlockEntity extends BlockEntity implements MenuProvider {

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        SpellEditorScreenHandler handler = new SpellEditorScreenHandler(syncId, inv, this);
        
        // 打开 GUI 时，如果已有磁盘则加载程序并同步到客户端
        ItemStack existingDisk = getDiskStack();
        if (!existingDisk.isEmpty() && existingDisk.getItem() instanceof SpellDiskItem) {
            handler.loadProgramFromDisk(existingDisk);
        }
        
        return handler;
    }
}
```

**关键点**：
- 在 `createMenu()` 中主动检查并加载磁盘程序
- 移除构造函数中的重复加载逻辑（避免加载两次）

### 3. SpellEditorScreen 添加静态引用和更新方法

```java
public class SpellEditorScreen extends AbstractContainerScreen<SpellEditorScreenHandler> {
    
    // 静态字段：当前打开的编辑器实例（用于网络包接收）
    private static SpellEditorScreen currentInstance = null;
    
    /** 获取当前打开的编辑器实例（供网络包接收器使用） */
    public static SpellEditorScreen getCurrentInstance() {
        return currentInstance;
    }

    @Override
    protected void init() {
        super.init();
        
        // 设置当前实例引用（用于网络包接收）
        currentInstance = this;
        
        // ... 初始化 Widget
    }

    /**
     * 从服务端同步程序列表
     * 由网络包接收器调用
     */
    public void updateProgramFromServer(List<Executable> program) {
        this.menu.setProgramFromServer(program);
        programListWidget.clearSelection();
    }

    /**
     * Screen 关闭时清理静态引用
     */
    @Override
    public void onClose() {
        currentInstance = null;
        super.onClose();
    }
}
```

**关键点**：
- 使用静态字段 `currentInstance` 追踪当前打开的编辑器
- `init()` 中设置引用，`onClose()` 中清理
- 提供 `getCurrentInstance()` 供网络包接收器访问

### 4. RelayClientNetworking 接收同步包并更新 UI

```java
public class RelayClientNetworking {

    public static void register() {
        // 注册 S2C_SyncSpellDiskPayload 接收器
        ClientPlayNetworking.registerGlobalReceiver(
            S2C_SyncSpellDiskPayload.TYPE,
            (payload, context) -> {
                CompoundTag programTag = payload.programNbt();
                ListTag programList = programTag.getList("program")
                    .orElse(new ListTag());
                
                try {
                    List<Executable> program = ProgramCompiler.fromNbt(programList);
                    
                    // 如果有打开的编辑器，更新其程序列表
                    SpellEditorScreen screen = SpellEditorScreen.getCurrentInstance();
                    if (screen != null) {
                        context.client().execute(() -> {
                            screen.updateProgramFromServer(program);
                        });
                    }
                } catch (ProgramCompiler.CompilationException e) {
                    e.printStackTrace();
                }
            }
        );
    }
}
```

**关键点**：
- `context.client().execute()` 确保在主线程更新 UI
- 通过 `getCurrentInstance()` 获取当前编辑器
- 解析 NBT 并调用 `updateProgramFromServer()`

## 网络包注册

### 服务端注册 S2C 包类型

```java
// RelayServerNetworking.java
public static void register() {
    // 注册 S2C 包类型（必须在服务端）
    PayloadTypeRegistry.clientboundPlay().register(
        S2C_SyncSpellDiskPayload.TYPE,
        S2C_SyncSpellDiskPayload.CODEC
    );
    
    // 客户端只需注册接收器，不需要注册包类型
}
```

### S2C_SyncSpellDiskPayload 定义

```java
public record S2C_SyncSpellDiskPayload(CompoundTag programNbt) implements CustomPacketPayload {
    public static final Type<S2C_SyncSpellDiskPayload> TYPE = 
        new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "s2c_sync_spell_disk"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SyncSpellDiskPayload> CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG,
            S2C_SyncSpellDiskPayload::programNbt,
            S2C_SyncSpellDiskPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

## 设计决策

### 为什么使用服务端主动同步？

| 方案 | 优点 | 缺点 |
|------|------|------|
| **服务端主动同步** | 架构正确，数据权威在服务端 | 需要额外的网络包 |
| 客户端主动读取 | 实现简单 | 客户端可能访问不到 BlockEntity |
| 混合方案 | 响应快 | 需要处理状态冲突 |

**选择服务端主动同步的原因**：
1. **数据权威性**：服务端是数据的权威来源
2. **同步可靠性**：确保客户端和服务端状态一致
3. **架构清晰**：符合 Minecraft 的服务端权威模型

### 为什么使用静态字段追踪 Screen？

- 网络包接收器是静态方法，无法直接访问 Screen 实例
- 使用静态字段 `currentInstance` 提供全局访问点
- 在 `init()` 和 `onClose()` 中管理生命周期

### 为什么需要 `context.client().execute()`？

- 网络包接收器在**网络线程**执行
- GUI 更新必须在**主线程**执行
- `context.client().execute()` 调度到主线程

## 常见问题排查

### 问题：编译错误 "isClientSide 在 Level 中是 private 访问控制"

**原因**：26.1.2 中 `isClientSide` 是方法而非字段

**解决**：
```java
// 错误写法
if (!level.isClientSide) { ... }

// 正确写法
if (!level.isClientSide()) { ... }
```

### 问题：打开编辑器时显示空列表

**排查步骤**：
1. 检查 `createMenu()` 是否调用 `loadProgramFromDisk()`
2. 检查 `syncProgramToClient()` 是否在服务端执行
3. 检查网络包是否正确注册
4. 检查客户端是否收到包（添加日志）

### 问题：网络包接收器不触发

**原因**：
- S2C 包类型未在服务端注册
- 客户端未注册接收器

**解决**：
```java
// 服务端注册包类型
PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);

// 客户端注册接收器
ClientPlayNetworking.registerGlobalReceiver(TYPE, handler);
```

### 问题：多个玩家同时打开编辑器

**解决**：`syncProgramToClient()` 使用 `players().stream()` 过滤：
```java
level.players().stream()
    .filter(p -> p.containerMenu == this)  // 只同步给打开此编辑器的玩家
    .filter(p -> p instanceof ServerPlayer)
    .findFirst()
    .ifPresent(p -> send(...));
```

## 相关文件

- `SpellEditorScreenHandler.java` - 添加 `syncProgramToClient()` 和 `setProgramFromServer()`
- `SpellEditorBlockEntity.java` - `createMenu()` 中触发加载
- `SpellEditorScreen.java` - 静态字段和 `updateProgramFromServer()`
- `RelayClientNetworking.java` - 网络包接收器
- `S2C_SyncSpellDiskPayload.java` - S2C 同步包定义
- `RelayServerNetworking.java` - 服务端包类型注册

## 适用场景

此模式适用于所有需要**服务端主动同步 GUI 状态**的场景：

- 自定义容器的物品栏同步
- 编辑器内容同步
- 进度条/能量条同步
- 其他非标准 Minecraft 同步机制的状态

**核心思想**：服务端是数据权威，通过 S2C 包主动同步到客户端。
