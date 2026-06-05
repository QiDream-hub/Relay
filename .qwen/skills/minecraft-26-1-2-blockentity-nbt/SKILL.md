---
name: minecraft-26-1-2-blockentity-nbt
description: Minecraft 26.1.2 中 BlockEntity 和 Entity 的 NBT 读写系统变更及 DataComponent 替代方案
source: auto-skill
extracted_at: '2026-05-30T12:30:04.081Z'
---

# Minecraft 26.1.2 BlockEntity 和 Entity NBT 系统变更

## 核心变更概述

Minecraft 26.1.2 版本中，`BlockEntity` 和 `Entity` 的 NBT 读写系统发生了重大变化：

1. **`saveAdditional` 和 `loadAdditional` 方法签名变更** - 参数从 `CompoundTag` 改为 `ValueOutput`/`ValueInput`
2. **`ItemStack` 的 NBT 方法废弃** - 推荐使用 `DataComponent` 系统
3. **`getUpdateTag()` 方法签名变更** - 需要传入 `HolderLookup.Provider`

## ValueOutput/ValueInput 位置

```java
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
```

这两个类位于 `net.minecraft.world.level.storage` 包中，而非 `net.minecraft.nbt`。

## Entity NBT 读写

26.1.2 版本中 `Entity` 类有四个新的抽象方法必须实现：

```java
public class SimpleEntityShell extends EntityShell {
    
    // 26.1.2 新增的抽象方法 - 保存额外数据
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("lifetime", getLifetime());
        output.putInt("energy", getEnergy());
    }

    // 26.1.2 新增的抽象方法 - 读取额外数据
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // 从 ValueInput 读取数据
        // 注意：ValueInput 的 getter 也返回 Optional
    }
    
    // 26.1.2 新增的抽象方法 - 服务端伤害处理
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // 处理服务端伤害逻辑
        return false; // 返回 false 表示不受伤害
    }
    
    // 26.1.2 新增的抽象方法 - 定义同步数据
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // 定义需要同步到客户端的数据
        // SynchedEntityData 在 net.minecraft.network.syncher 包中
    }
}
```

### 注意事项

1. **`ValueOutput` 和 `ValueInput` 的方法**：
   ```java
   // ValueOutput
   output.putInt("key", value);
   output.putDouble("key", value);
   output.putString("key", value);
   
   // ValueInput
   input.getInt("key").orElse(0); // 返回 Optional
   input.getDouble("key").orElse(0.0);
   input.getString("key").orElse("");
   ```

2. **`SynchedEntityData.Builder` 在 `net.minecraft.network.syncher` 包中**：
   ```java
   import net.minecraft.network.syncher.SynchedEntityData;
   
   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
       // 无需同步数据时留空
   }
   ```

3. **`hurtServer` 方法在 `ServerLevel` 中调用**：
   ```java
   import net.minecraft.server.level.ServerLevel;
   import net.minecraft.world.damagesource.DamageSource;
   ```

## BlockEntity NBT 读写

### 旧写法 (1.20.1 及之前)

```java
@Override
protected void writeNbt(CompoundTag nbt) {
    super.writeNbt(nbt);
    nbt.putInt("energy", energy);
    nbt.put("item", itemStack.writeNbt(new CompoundTag()));
}

@Override
protected void readNbt(CompoundTag nbt) {
    super.readNbt(nbt);
    energy = nbt.getInt("energy");
    itemStack = ItemStack.fromNbt(nbt.getCompound("item"));
}
```

### 新写法 (26.1.2) - 使用 ValueInput/ValueOutput

```java
@Override
protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
    super.saveAdditional(tag, provider);
    tag.putInt("energy", energy);
    tag.put("item", itemStack.save(provider, new CompoundTag()));
}

@Override
protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
    super.loadAdditional(tag, provider);
    energy = tag.getInt("energy").orElse(0);
    itemStack = ItemStack.parse(provider, tag.getCompound("item")).orElse(ItemStack.EMPTY);
}
```

### 注意事项

1. **`CompoundTag` 的 getter 方法返回 `Optional`**：
   ```java
   // 旧写法
   int value = nbt.getInt("key");
   
   // 新写法
   int value = nbt.getInt("key").orElse(0);
   ```

2. **`ItemStack.save()` 和 `ItemStack.parse()` 需要 `HolderLookup.Provider`**：
   ```java
   // 保存
   tag.put("item", itemStack.save(provider, new CompoundTag()));
   
   // 读取
   itemStack = ItemStack.parse(provider, tag.getCompound("item")).orElse(ItemStack.EMPTY);
   ```

3. **`BlockEntity` 的 `setChanged()` 替代 `markDirty()`**：
   ```java
   // 旧写法
   markDirty();
   
   // 新写法
   setChanged();
   ```

## 简化实现方案

由于 26.1.2 的 NBT 系统变化较大，如果暂时不需要完整的 NBT 持久化，可以简化实现：

```java
public class ShellBlockEntity extends BlockEntity {
    
    public ShellBlockEntity(BlockPos pos, BlockState state) {
        super(MY_BLOCK_ENTITY_TYPE, pos, state);
    }
    
    // 暂时不覆盖 saveAdditional/loadAdditional
    // 使用默认实现，仅保存基础 BlockEntity 数据
    
    // 状态变更时调用
    public void updateState() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
}
```

## DataComponent 系统 (替代 ItemStack NBT)

26.1.2 版本推荐使用 `DataComponent` 系统替代 `ItemStack` 的 NBT：

### 旧写法 (使用 NBT)

```java
ItemStack stack = new ItemStack(Items.DIAMOND);
stack.getOrCreateTag().putInt("energy", 100);
int energy = stack.getTag().getInt("energy").orElse(0);
```

### 新写法 (使用 DataComponent)

```java
// 1. 定义自定义组件类型
public static final DataComponentType<Integer> ENERGY = DataComponentType.<Integer>builder()
    .persistent(Codec.INT)
    .build();

// 2. 注册组件
Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, 
    Identifier.fromNamespaceAndPath(MOD_ID, "energy"), ENERGY);

// 3. 使用组件
ItemStack stack = new ItemStack(Items.DIAMOND);
stack.set(ENERGY, 100);
Integer energy = stack.get(ENERGY);
```

### CustomData 简化方案

如果不需要自定义组件类型，可以使用 `CustomData`：

```java
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;

// 保存数据
ItemStack stack = new ItemStack(Items.DIAMOND);
CompoundTag tag = new CompoundTag();
tag.putInt("energy", 100);
stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

// 读取数据
CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
if (customData != null) {
    CompoundTag tag = customData.copyTag();
    int energy = tag.getInt("energy").orElse(0);
}
```

## getUpdateTag() 方法变更

### 旧写法

```java
@Override
public CompoundTag getUpdateTag() {
    CompoundTag tag = super.getUpdateTag();
    saveAdditional(tag);
    return tag;
}
```

### 新写法 (26.1.2)

```java
@Override
public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
    CompoundTag tag = super.getUpdateTag(provider);
    saveAdditional(tag, provider);
    return tag;
}
```

## 完整示例

### ShellBlockEntity 完整实现

```java
public class ShellBlockEntity extends BlockEntity {
    private int energy = 0;
    private final ItemStack[] inventory = new ItemStack[4];
    
    public ShellBlockEntity(BlockPos pos, BlockState state) {
        super(MY_BLOCK_ENTITY_TYPE, pos, state);
        Arrays.fill(inventory, ItemStack.EMPTY);
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("energy", energy);
        
        // 保存物品栏
        for (int i = 0; i < inventory.length; i++) {
            if (!inventory[i].isEmpty()) {
                tag.put("inventory_" + i, inventory[i].save(provider, new CompoundTag()));
            }
        }
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        energy = tag.getInt("energy").orElse(0);
        
        // 加载物品栏
        for (int i = 0; i < inventory.length; i++) {
            String key = "inventory_" + i;
            if (tag.contains(key)) {
                inventory[i] = ItemStack.parse(provider, tag.getCompound(key)).orElse(ItemStack.EMPTY);
            } else {
                inventory[i] = ItemStack.EMPTY;
            }
        }
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveAdditional(tag, provider);
        return tag;
    }
    
    public void updateState() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
```

## 常见问题排查

### 问题 1: 编译错误 "不兼容的类型：CompoundTag 无法转换为 ValueOutput"

**原因**: `saveAdditional` 方法签名已变更

**解决**: 使用 `HolderLookup.Provider` 参数版本的方法，或暂时不覆盖该方法

### 问题 2: 编译错误 "找不到符号：方法 markDirty()"

**原因**: `markDirty()` 已重命名为 `setChanged()`

**解决**: 全局替换 `markDirty()` 为 `setChanged()`

### 问题 3: 编译错误 "方法不会覆盖或实现超类型的方法"

**原因**: `load()` 方法已重名为 `loadAdditional()`

**解决**: 使用方法名 `loadAdditional` 并确保参数正确

### 问题 4: CompoundTag getter 返回 Optional

**原因**: 26.1.2 中所有 getter 方法返回 `Optional` 类型

**解决**: 使用 `.orElse(default)` 或 `.get()` 处理：
```java
int value = tag.getInt("key").orElse(0);
boolean exists = tag.contains("key"); // 先检查是否存在
```

## 验证步骤

修复完成后，运行以下命令验证：

```bash
./gradlew build
```

常见错误及解决方案：
1. `不兼容的类型：CompoundTag 无法转换为 ValueOutput` - 添加 `HolderLookup.Provider` 参数
2. `找不到符号：方法 markDirty()` - 替换为 `setChanged()`
3. `方法不会覆盖或实现超类型的方法` - 检查方法名和参数签名
