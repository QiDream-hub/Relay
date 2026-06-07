---
name: minecraft-26-1-2-blockentity-nbt
description: Minecraft 26.1.2 中 BlockEntity 和 Entity 的 NBT 读写系统变更及 DataComponent 替代方案
source: auto-skill
extracted_at: '2026-06-07T16:40:00.000Z'
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

### 26.1.2 实际使用的 API - ValueInput/ValueOutput + Codec

根据 Fabric 官方教程和实际实现，26.1.2 的 `BlockEntity` 序列化使用 `ValueInput`/`ValueOutput` 配合 `Codec`：

```java
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.nbt.CompoundTag;

@Override
protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    
    // 保存简单类型
    output.putInt("energy", energy);
    output.putBoolean("initialized", true);
    
    // 保存复杂类型 - 使用 Codec
    CompoundTag machineTag = serializeStateMachine();
    output.store("stateMachine", CompoundTag.CODEC, machineTag);
    
    // 保存物品栏 - 使用 CompoundTag 作为中间格式
    CompoundTag inventoryTag = new CompoundTag();
    for (int i = 0; i < inventory.length; i++) {
        if (!inventory[i].isEmpty()) {
            CompoundTag slotTag = new CompoundTag();
            // 保存物品 ID 和数量（简化版，未保存 DataComponent）
            var itemId = BuiltInRegistries.ITEM.getKey(inventory[i].getItem());
            if (itemId != null) {
                slotTag.putString("id", itemId.toString());
                slotTag.putInt("Count", inventory[i].getCount());
                inventoryTag.put("slot_" + i, slotTag);
            }
        }
    }
    output.store("inventory", CompoundTag.CODEC, inventoryTag);
}

@Override
protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    
    // 加载简单类型
    energy = input.getIntOr("energy", 0);
    boolean initialized = input.getBooleanOr("initialized", false);
    
    // 加载复杂类型 - 使用 Codec
    input.read("stateMachine", CompoundTag.CODEC).ifPresent(tag -> {
        deserializeStateMachine((CompoundTag) tag);
    });
    
    // 加载物品栏
    input.read("inventory", CompoundTag.CODEC).ifPresent(inventoryTag -> {
        for (int i = 0; i < inventory.length; i++) {
            inventoryTag.getCompound("slot_" + i).ifPresent(slotTag -> {
                slotTag.getString("id").ifPresent(itemIdStr -> {
                    slotTag.getInt("Count").ifPresent(count -> {
                        var itemId = Identifier.tryParse(itemIdStr);
                        if (itemId != null) {
                            var itemOpt = BuiltInRegistries.ITEM.getOptional(itemId);
                            itemOpt.ifPresent(item -> {
                                inventory[i] = new ItemStack(item, count);
                            });
                        }
                    });
                });
            });
        }
    });
}
```

### ValueInput/ValueOutput API

```java
// ValueOutput - 写入数据
output.putInt(String key, int value);
output.putBoolean(String key, boolean value);
output.putDouble(String key, double value);
output.putString(String key, String value);
output.store(String key, Codec<T> codec, T value);  // 复杂类型

// ValueInput - 读取数据
int getIntOr(String key, int defaultValue);
boolean getBooleanOr(String key, boolean defaultValue);
double getDoubleOr(String key, double defaultValue);
String getStringOr(String key, String defaultValue);
Optional<T> read(String key, Codec<T> codec);  // 复杂类型
Optional<CompoundTag> getCompound(String key);  // 嵌套 CompoundTag
```

### 注意事项

1. **`CompoundTag` 的 getter 方法返回 `Optional`**：
   ```java
   // 26.1.2 新 API
   int value = tag.getInt("key").orElse(0);
   Optional<ListTag> listOpt = tag.getList("key");
   Optional<CompoundTag> compoundOpt = tag.getCompound("key");
   ```

2. **`ValueInput`/`ValueOutput` 位于 `net.minecraft.world.level.storage` 包**：
   ```java
   import net.minecraft.world.level.storage.ValueOutput;
   import net.minecraft.world.level.storage.ValueInput;
   ```

3. **`CompoundTag.CODEC` 是内置的 Codec**，可以直接用于 `output.store()` 和 `input.read()`

4. **物品栏序列化简化方案**（由于 DataComponent 系统复杂）：
   - 只保存物品 ID 和数量
   - 使用 `BuiltInRegistries.ITEM.getKey()` 获取物品 ID
   - 使用 `BuiltInRegistries.ITEM.getOptional()` 查找物品
   - DataComponent 的完整序列化标记为 TODO

5. **`BlockEntity` 的 `setChanged()` 替代 `markDirty()`**：
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

### 新写法 (26.1.2)

```java
@Override
public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
    return saveWithoutMetadata(registryLookup);
}
```

这个方法用于客户端同步 - 当玩家加载区块或移动到有方块实体的区块时，客户端会收到正确的数据。

### 配合 setChanged() 广播更新

```java
@Override
public void setChanged() {
    super.setChanged();
    
    if (level == null) return;
    
    BlockState state = getBlockState();
    level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
}
```

## 完整示例

### ShellBlockEntity 完整实现 (26.1.2)

```java
public class ShellBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStack[] inventory = new ItemStack[4];
    private final StateMachine stateMachine;
    private int energy;

    public ShellBlockEntity(BlockPos pos, BlockState state) {
        super(MY_BLOCK_ENTITY_TYPE, pos, state);
        Arrays.fill(inventory, ItemStack.EMPTY);
        this.stateMachine = new StateMachine(1024);
        this.energy = 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // 保存物品栏 - 使用 CompoundTag 作为中间格式
        CompoundTag inventoryTag = new CompoundTag();
        for (int i = 0; i < inventory.length; i++) {
            if (!inventory[i].isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                var itemId = BuiltInRegistries.ITEM.getKey(inventory[i].getItem());
                if (itemId != null) {
                    slotTag.putString("id", itemId.toString());
                    slotTag.putInt("Count", inventory[i].getCount());
                    inventoryTag.put("slot_" + i, slotTag);
                }
            }
        }
        output.store("inventory", CompoundTag.CODEC, inventoryTag);

        // 保存能量
        output.putInt("energy", energy);

        // 保存状态机状态 - 使用 CompoundTag.CODEC
        CompoundTag machineTag = StateMachineNbtSerializer.INSTANCE.serialize(stateMachine);
        output.store("stateMachine", CompoundTag.CODEC, machineTag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // 加载物品栏
        input.read("inventory", CompoundTag.CODEC).ifPresent(inventoryTag -> {
            for (int i = 0; i < inventory.length; i++) {
                inventoryTag.getCompound("slot_" + i).ifPresent(slotTag -> {
                    slotTag.getString("id").ifPresent(itemIdStr -> {
                        slotTag.getInt("Count").ifPresent(count -> {
                            var itemId = Identifier.tryParse(itemIdStr);
                            if (itemId != null) {
                                var itemOpt = BuiltInRegistries.ITEM.getOptional(itemId);
                                itemOpt.ifPresent(item -> {
                                    inventory[i] = new ItemStack(item, count);
                                });
                            }
                        });
                    });
                });
            }
        });

        // 加载能量
        energy = input.getIntOr("energy", 0);

        // 加载状态机状态
        input.read("stateMachine", CompoundTag.CODEC).ifPresent(tag -> {
            StateMachineNbtSerializer.INSTANCE.deserialize(stateMachine, (CompoundTag) tag);
        });
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }
}
```

### StateMachineNbtSerializer 实现

```java
public class StateMachineNbtSerializer {
    public static final StateMachineNbtSerializer INSTANCE = new StateMachineNbtSerializer();

    public CompoundTag serialize(StateMachine machine) {
        CompoundTag tag = new CompoundTag();

        // 保存程序栈
        ListTag programList = new ListTag();
        for (Executable exe : machine.getProgramStackSnapshot()) {
            OperationRegistry.serializeToNbt(exe).ifPresent(programList::add);
        }
        tag.put("programStack", programList);

        // 保存数据栈
        ListTag dataList = new ListTag();
        for (Executable data : machine.getDataStackSnapshot()) {
            OperationRegistry.serializeToNbt(data).ifPresent(dataList::add);
        }
        tag.put("dataStack", dataList);

        tag.putBoolean("hasWorldInteractor", machine.hasWorldInteractor());
        tag.putInt("maxStackSize", machine.getMaxStackSize());

        return tag;
    }

    public void deserialize(StateMachine machine, CompoundTag tag) {
        // 加载程序栈
        ListTag programList = tag.getList("programStack").orElse(new ListTag());
        List<Executable> programStack = new ArrayList<>();
        for (Tag element : programList) {
            if (element instanceof CompoundTag compoundTag) {
                OperationRegistry.deserializeFromNbt(compoundTag).ifPresent(programStack::add);
            }
        }
        Collections.reverse(programStack);
        machine.loadProgram(programStack);

        // 加载数据栈
        ListTag dataList = tag.getList("dataStack").orElse(new ListTag());
        List<Executable> dataStack = new ArrayList<>();
        for (Tag element : dataList) {
            if (element instanceof CompoundTag compoundTag) {
                OperationRegistry.deserializeFromNbt(compoundTag).ifPresent(dataStack::add);
            }
        }
        Collections.reverse(dataStack);
        for (Executable data : dataStack) {
            machine.pushData(data);
        }

        machine.setHasWorldInteractor(tag.getBoolean("hasWorldInteractor").orElse(false));
        machine.setMaxStackSize(tag.getInt("maxStackSize").orElse(1024));
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
