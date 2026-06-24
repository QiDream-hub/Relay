---
name: shell-entity-types
description: Relay 模组的三种 Shell 形态及其与玩家的关系 - 方块外壳、实体外壳、工具外壳的统一接口和交互模式
source: auto-skill
extracted_at: '2026-06-24T07:58:12.968Z'
---

# Shell 实体类型与玩家关系

## 三种 Shell 形态

Relay 模组中有三种外壳（Shell）形态，都实现统一的 `ShellContainer` 接口：

| 类型 | 类 | 形态 | 创建方式 |
|------|-----|------|----------|
| **方块外壳** | `ShellBlockEntity` | 放置的方块 | 玩家放置方块 |
| **实体外壳** | `EntityShell` → `SimpleEntityShell` | 可移动的实体 | 右键召唤 |
| **工具外壳** | `ToolShellItem` | 手持物品 | 物品栏中的物品 |

## 统一接口：ShellContainer

```java
public interface ShellContainer {
    // 4 个插槽：核心、磁盘、能量模块、世界交互器
    int CORE_SLOT = 0;
    int DISK_SLOT = 1;
    int ENERGY_SLOT = 2;
    int INTERACTOR_SLOT = 3;
    
    ItemStack getInventorySlot(int slot);
    void setInventorySlot(int slot, ItemStack stack);
    
    // 状态机访问
    StateMachine getStateMachine();
    
    // 所有者（主人）管理
    Entity getOwner();
    void setOwner(Entity owner);
    
    // 运行状态
    int getCoreCount();
    int getInterval();
    boolean isEnabled();
    void setEnabled(boolean enabled);
    boolean hasWorldInteractor();
}
```

## 玩家与 Shell 的交互链路

### 方块外壳（标准实现）

```
玩家右键方块
    ↓
ShellBlock.useWithoutItem()
    ↓
player.openMenu((MenuProvider) blockEntity)
    ↓
ShellBlockEntity.createMenu() → ShellScreenHandler
    ↓
客户端打开 ShellScreen
```

**关键代码**：
```java
// ShellBlock.java
@Override
protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
    if (world.isClientSide()) {
        return InteractionResult.SUCCESS;
    }
    BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity instanceof ShellBlockEntity shell) {
        player.openMenu(shell);
    }
    return InteractionResult.CONSUME;
}

// ShellBlockEntity.java
public class ShellBlockEntity extends BlockEntity implements MenuProvider, ShellContainer {
    @Override
    public Component getDisplayName() {
        return Component.literal("外壳");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new ShellScreenHandler(syncId, inv, this);
    }
}
```

### 实体外壳

```
玩家右键实体
    ↓
EntityShell.interact()
    ↓
player.openMenu(this)  // this 是 EntityShell，实现 MenuProvider
    ↓
EntityShell.createMenu() → ShellScreenHandler
    ↓
客户端打开 ShellScreen
```

**关键代码**：
```java
// EntityShell.java
public abstract class EntityShell extends Entity implements MenuProvider, ShellContainer {
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("实体外壳");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new ShellScreenHandler(syncId, inv, this);
    }
}
```

### 工具外壳

```
玩家手持右键
    ↓
ToolShellItem.interact()
    ↓
player.openMenu(new ToolShellMenuProvider(stack, hand))
    ↓
客户端打开工具外壳 GUI
```

## 所有者（Owner）系统

`ShellContainer` 接口提供所有者管理，用于追踪 Shell 的拥有者：

```java
// 接口定义
Entity getOwner();
void setOwner(Entity owner);

// 方块外壳实现 - NBT 持久化
@Override
protected void saveAdditional(ValueOutput output) {
    if (owner != null) {
        output.putString("owner", owner.getUUID().toString());
    }
}

@Override
protected void loadAdditional(ValueInput input) {
    String uuidStr = input.getString("owner").orElse("");
    if (!uuidStr.isEmpty()) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            if (level != null && !level.isClientSide()) {
                owner = level.getEntity(uuid);
            }
        } catch (IllegalArgumentException e) {
            // UUID 格式错误，忽略
        }
    }
}
```

**用途**：
- 追踪 Shell 的拥有者（通常是放置/召唤它的玩家）
- 通过 UUID 持久化，支持世界重加载后恢复
- 权限检查、能量消耗归属等

## EntityIota 类型 - 实体引用存储（2026-06-24 强化版）

`EntityIota` 用于在法术程序中存储实体引用，支持跨维度持久化：

```java
public class EntityIota extends Data {
    private final UUID entityId;           // 实体 UUID，用于序列化
    private final String worldId;          // 世界 ID 字符串，如 "minecraft:overworld"
    private transient Entity entityRef;    // 运行时缓存引用（不序列化）

    // 从实体创建（存储 UUID + 世界 ID + 引用）
    public static EntityIota from(Entity entity, Level world) {
        if (entity == null) {
            return new EntityIota(null, null, null);
        }
        String worldId = entity.level().dimension().registry().toString();
        return new EntityIota(entity.getUUID(), worldId, entity);
    }

    // 从 UUID 和世界 ID 创建（用于反序列化，不保持引用）
    public static EntityIota fromUuid(UUID entityId, String worldId) {
        return new EntityIota(entityId, worldId, null);
    }

    // 获取实体引用（通过世界查询，支持延迟查询）
    public Entity getEntity(Level world) {
        // 先验证缓存引用是否有效
        if (entityRef != null && !entityRef.isRemoved()) {
            return entityRef;
        }
        // 缓存失效，通过 UUID 查询
        if (entityId == null || world == null) {
            return null;
        }
        return world.getEntity(entityId);
    }

    // 获取 UUID
    public UUID getUuid() { return entityId; }

    // 获取世界 ID 字符串
    public String getWorldId() { return worldId; }

    // 是否是 null 实体
    public boolean isNull() {
        return entityId == null && worldId == null && entityRef == null;
    }
}
```

**设计要点**：
1. **三重存储**：UUID + 世界 ID 用于持久化，直接引用用于运行时
2. **跨维度支持**：通过世界 ID 区分不同维度的实体
3. **延迟查询**：反序列化时不立即查询实体，`getEntity(Level)` 时才查询
4. **缓存验证**：查询前先验证缓存引用是否仍然有效（`!isRemoved()`）
5. **null 支持**：`from(null, world)` 创建 null 实体

**序列化格式**：
```json
{
  "type": "relay:entity",
  "value": {
    "uuid": "12345678-1234-1234-1234-123456789abc",
    "world": "minecraft:overworld"
  }
}
```

**使用场景**：
- `GetSelfOp`：返回 Shell 自身对应的实体（方块外壳返回 null）
- `GetOwnerOp`：返回 Shell 的所有者实体
- 法术程序中传递和存储实体引用

## 关键设计模式

### 1. 三种形态统一接口
通过 `ShellContainer` 接口实现代码复用，`ShellScreenHandler` 可以处理任意 Shell 类型。

### 2. GUI 打开标准模式
- 实现 `MenuProvider` 接口
- 右键时调用 `player.openMenu()`
- 无需方块/物品直接声明 GUI

### 3. 所有者追踪
- 运行时：直接存储 `Entity` 引用
- 持久化：存储 UUID
- 加载时：通过 UUID 从世界恢复引用

### 4. 实体引用统一（2026-06-24）
- **移除 `ContainerIota`**：不再需要单独的容器引用类型
- **`GetSelfOp` 和 `GetOwnerOp` 都返回 `EntityIota`**：
  - `GetSelfOp`：实体外壳返回自身，方块外壳返回 null
  - `GetOwnerOp`：返回所有者实体
- **延迟查询**：`EntityIota.getEntity(Level)` 需要世界参数才能获取实体引用
- **世界上下文**：`ShellTickHandler` 在上下文中设置 `world` 供操作使用

```java
// ShellTickHandler.java - 设置世界上下文
if (container instanceof net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
    stateMachine.setContext("world", blockEntity.getLevel());
} else if (container instanceof net.minecraft.world.entity.Entity entity) {
    stateMachine.setContext("world", entity.level());
}

// GetSelfOp.java - 返回 EntityIota
ShellContainer container = executor.getContext("shellContainer", ShellContainer.class);
Level world = executor.getContext("world", Level.class);
Entity selfEntity = (container instanceof Entity entity) ? entity : null;
executor.pushData(EntityIota.from(selfEntity, world));
```

## 实现状态

| 功能 | 状态 |
|------|------|
| `ShellBlockEntity` 方块外壳 | ✅ 完整实现 |
| `EntityShell` 实体外壳 | ⚠️ 抽象类已实现，`SimpleEntityShell` 简化实现 |
| `ToolShellItem` 工具外壳 | ⚠️ 简化实现（DataComponent 待适配） |
| `ShellContainer` 统一接口 | ✅ 完整实现 |
| 所有者系统 | ✅ 方块外壳完整，实体外壳部分 |
| `EntityIota` 类型（强化版） | ✅ 支持跨维度 + 延迟查询 |
| `ContainerIota` | ❌ 已移除（2026-06-24） |
| `GetSelfOp` 返回 `EntityIota` | ✅ 已更新 |
| `GetOwnerOp` 返回 `EntityIota` | ✅ 已更新 |
