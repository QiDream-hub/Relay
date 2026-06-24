---
name: relay-entity-blockentity-separation
description: Relay 模组中 EntityIota 和 BlockEntityIota 分离设计 - Entity 和 BlockEntity 是并行体系，使用独立类型存储引用
source: auto-skill
extracted_at: '2026-06-24T09:15:01.800Z'
---

# Relay 模组 Entity/BlockEntity 分离设计

## 核心设计原则

**Entity 和 BlockEntity 是 Minecraft 中两个并行的体系，应该使用独立的 Iota 类型存储引用。**

### 为什么分离？

1. **API 隔离**：`Entity` 和 `BlockEntity` 没有共同的父类，无法用统一方式访问
2. **标识方式不同**：
   - `Entity` 通过 `UUID` 标识（`entity.getUUID()`）
   - `BlockEntity` 通过 `BlockPos` 标识（Minecraft 没有提供通过 UUID 查找 BlockEntity 的 API）
3. **查询方式不同**：
   - `Entity`：`world.getEntity(uuid)`
   - `BlockEntity`：`world.getBlockEntity(blockPos)`

## 类型设计

### EntityIota

```java
public class EntityIota extends Data {
    private final UUID uuid;
    private final String worldId;
    private transient Entity entityRef;  // 运行时缓存
    
    // 从 Entity 创建
    public static EntityIota from(Entity entity, Level world) {
        String worldId = entity.level().dimension().registry().toString();
        return new EntityIota(entity.getUUID(), worldId, entity);
    }
    
    // 获取实体（通过 UUID 查询）
    public Entity getEntity(Level world) {
        if (entityRef != null && !entityRef.isRemoved()) {
            return entityRef;
        }
        return uuid != null && world != null ? world.getEntity(uuid) : null;
    }
}
```

### BlockEntityIota

```java
public class BlockEntityIota extends Data {
    private final BlockPos blockPos;
    private final String worldId;
    private transient BlockEntity blockEntityRef;  // 运行时缓存
    
    // 从 BlockEntity 创建
    public static BlockEntityIota from(BlockEntity blockEntity, Level world) {
        String worldId = blockEntity.getLevel().dimension().registry().toString();
        BlockPos pos = blockEntity.getBlockPos().immutable();
        return new BlockEntityIota(pos, worldId, blockEntity);
    }
    
    // 获取方块实体（通过 BlockPos 查询）
    public BlockEntity getBlockEntity(Level world) {
        if (blockEntityRef != null && !blockEntityRef.isRemoved()) {
            return blockEntityRef;
        }
        return blockPos != null && world != null ? world.getBlockEntity(blockPos) : null;
    }
}
```

## GetSelfOp 实现模式

`GetSelfOp` 根据 Shell 类型返回不同的 Iota：

```java
public class GetSelfOp extends Spell {
    @Override
    public void execute(StateMachine executor) {
        ShellContainer container = executor.getContext("shellContainer", ShellContainer.class);
        Level world = executor.getContext("world", Level.class);
        
        if (container instanceof Entity entity) {
            // 实体外壳 → EntityIota
            executor.pushData(EntityIota.from(entity, world));
        } else if (container instanceof BlockEntity blockEntity) {
            // 方块外壳 → BlockEntityIota
            executor.pushData(BlockEntityIota.from(blockEntity, world));
        }
    }
}
```

## 序列化格式

### EntityIota NBT
```java
{
  "world": "minecraft:overworld",
  "uuid": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

### BlockEntityIota NBT
```java
{
  "world": "minecraft:overworld",
  "x": 100,
  "y": 64,
  "z": 200
}
```

## 演进过程（经验教训）

### 尝试 1：统一使用 EntityIota
```java
// ❌ 失败：BlockEntity 没有 getUuid() 方法
UUID uuid = blockEntity.getUuid();

// ❌ 失败：Minecraft 没有 API 通过 UUID 查找 BlockEntity
world.getBlockEntity(uuid);  // 不存在
```

### 尝试 2：EntityIota 支持两种类型
```java
// ❌ 复杂：需要 type 字段区分 Entity/BlockEntity
// ❌ 不一致：Entity 用 UUID，BlockEntity 用 BlockPos
// ❌ 混乱：getEntity() 和 getBlockEntity() 在同一个类中
```

### 最终方案：独立类型
```java
// ✅ 清晰：EntityIota 只处理 Entity，BlockEntityIota 只处理 BlockEntity
// ✅ 一致：每个类型使用最适合的标识方式
// ✅ 扩展性好：可以为 BlockEntityIota 添加特有操作（get_block_type 等）
```

## 使用示例

```java
// 获取自身引用
get_self

// 方块外壳可以使用位置相关操作
get_self get_block_pos  // 获取方块位置
get_self get_block_type // 获取方块类型

// 实体外壳可以使用实体相关操作
get_self is_player      // 检查是否是玩家
get_self get_health     // 获取生命值
```

## 关键要点

1. **不要尝试统一 Entity 和 BlockEntity** - 它们是 Minecraft 中两个独立的体系
2. **使用最适合的标识方式** - Entity 用 UUID，BlockEntity 用 BlockPos
3. **运行时缓存很重要** - 避免重复查询世界
4. **世界 ID 用于跨维度** - 支持实体/方块实体在不同维度间的引用
