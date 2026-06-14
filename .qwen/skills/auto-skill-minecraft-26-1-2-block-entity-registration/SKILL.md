---
name: minecraft-26-1-2-block-entity-registration
description: Minecraft 26.1.2 方块与方块实体注册最佳实践 - 泛型 helper 方法与创造模式标签页注册
source: auto-skill
extracted_at: '2026-06-14T03:48:59.560Z'
---

# Minecraft 26.1.2 方块与方块实体注册最佳实践

基于官方示例与 Relay 项目对比分析得出的最佳实践。

## 核心模式

### 1. 方块注册表 - 使用 ResourceKey + Registry.register

**推荐模式（官方示例）：**

```java
public class ModBlocks {
    public static final Block COUNTER_BLOCK = register(
        "counter_block",
        CounterBlock::new,
        BlockBehaviour.Properties.of(),
        true
    );

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> blockFactory, 
                                  BlockBehaviour.Properties properties, boolean shouldRegisterItem) {
        ResourceKey<Block> blockKey = keyOfBlock(name);
        Block block = blockFactory.apply(properties.setId(blockKey));
        
        if (shouldRegisterItem) {
            ResourceKey<Item> itemKey = keyOfItem(name);
            BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
            Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        }

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static ResourceKey<Block> keyOfBlock(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, name));
    }

    private static ResourceKey<Item> keyOfItem(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name));
    }

    /**
     * 将方块添加到创造模式标签页
     */
    public static void setupItemGroups() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS).register((creativeTab) -> {
            creativeTab.accept(ModBlocks.COUNTER_BLOCK.asItem());
        });
    }

    public static void initialize() {
        setupItemGroups();
    }
}
```

**关键点：**
- ✅ 使用 `ResourceKey<Block>` 和 `ResourceKey<Item>` 分别注册方块和物品
- ✅ 通过 `shouldRegisterItem` 参数控制是否注册 BlockItem
- ✅ 使用 `Function` 工厂方法创建方块实例
- ✅ **必须实现 `setupItemGroups()` 方法将方块添加到创造模式标签页**
- ✅ 使用 `CreativeModeTabEvents.modifyOutputEvent()` 注册到指定标签页

**常用创造模式标签页：**
- `CreativeModeTabs.BUILDING_BLOCKS` - 建筑方块
- `CreativeModeTabs.NATURAL_BLOCKS` - 自然方块
- `CreativeModeTabs.FUNCTIONAL_BLOCKS` - 功能方块
- `CreativeModeTabs.TOOLS_AND_UTILITIES` - 工具和实用物品
- `CreativeModeTabs.COMBAT` - 战斗物品
- `CreativeModeTabs.FOOD_AND_DRINKS` - 食物和饮料

---

### 2. 方块实体注册表 - 使用泛型 helper 方法

**推荐模式（官方示例）：**

```java
public class ModBlockEntities {
    public static final BlockEntityType<CounterBlockEntity> COUNTER_BLOCK_ENTITY =
            register("counter", CounterBlockEntity::new, ModBlocks.COUNTER_BLOCK);

    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
            Block... blocks
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, name);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, 
            FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build(null));
    }

    public static void initialize() {}
}
```

**避免的模式（代码重复）：**

```java
// ❌ 不推荐：每个实体都要写 3 行重复代码
public class RelayBlockEntities {
    static {
        SHELL_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(...).build(null);
        Identifier shellId = Identifier.fromNamespaceAndPath(...);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, shellId, SHELL_BLOCK_ENTITY);
        
        SPELL_EDITOR_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(...).build(null);
        Identifier spellEditorId = Identifier.fromNamespaceAndPath(...);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, spellEditorId, SPELL_EDITOR_BLOCK_ENTITY);
    }
}
```

**优势：**
- ✅ 注册逻辑集中在一处，易于维护
- ✅ 使用泛型避免类型转换
- ✅ 每个实体注册只需一行代码
- ✅ 避免静态初始化块的复杂性

---

### 2. 方块实现 - codec() 方法

**推荐模式：**

```java
public class CounterBlock extends BaseEntityBlock {
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(CounterBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CounterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type
    ) {
        return createTickerHelper(type, ModBlockEntities.COUNTER_BLOCK_ENTITY, CounterBlockEntity::tick);
    }
}
```

**避免的模式：**

```java
// ❌ 不推荐：codec() 返回 null
@Override
protected MapCodec<? extends BaseEntityBlock> codec() {
    return null; // 会导致序列化问题
}
```

---

### 3. 方块实体实现 - NBT 序列化

**26.1.2 使用 ValueInput/ValueOutput 替代 CompoundTag：**

```java
public class CounterBlockEntity extends BlockEntity {
    private int clicks = 0;

    public CounterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COUNTER_BLOCK_ENTITY, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.putInt("clicks", this.clicks);
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.clicks = input.getIntOr("clicks", 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
```

**复杂对象序列化（如 ItemStack 数组）：**

**推荐模式（使用 ContainerHelper + NonNullList）：**

官方示例（DirtChestBlockEntity）使用 `ContainerHelper` 自动处理 DataComponent 系统：

```java
public class DirtChestBlockEntity extends BlockEntity implements MenuProvider {
    // ✅ 使用 NonNullList 替代数组
    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    @Override
    protected void saveAdditional(ValueOutput valueOutput) {
        super.saveAdditional(valueOutput);
        // ✅ ContainerHelper 自动处理 DataComponent 序列化
        ContainerHelper.saveAllItems(valueOutput, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput valueInput) {
        super.loadAdditional(valueInput);
        // ✅ ContainerHelper 自动处理 DataComponent 反序列化
        ContainerHelper.loadAllItems(valueInput, this.items);
    }
}
```

**避免的模式（手动序列化 CompoundTag）：**

```java
// ❌ 不推荐：手动处理每个物品槽，需要单独处理 DataComponent
private final ItemStack[] inventory = new ItemStack[4];

@Override
protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    CompoundTag inventoryTag = new CompoundTag();
    for (int i = 0; i < inventory.length; i++) {
        if (!inventory[i].isEmpty()) {
            CompoundTag slotTag = new CompoundTag();
            var itemId = BuiltInRegistries.ITEM.getKey(inventory[i].getItem());
            slotTag.putString("id", itemId.toString());
            slotTag.putInt("Count", inventory[i].getCount());
            // ❌ 需要手动处理 DataComponent
            inventoryTag.put("slot_" + i, slotTag);
        }
    }
    output.store("inventory", CompoundTag.CODEC, inventoryTag);
}
```

**ContainerHelper 的优势：**
- ✅ 自动处理 DataComponent 系统，无需手动序列化
- ✅ 支持完整的物品数据（包括附魔、自定义组件等）
- ✅ 代码简洁，减少出错可能
- ✅ 与 Minecraft 原版容器（箱子、熔炉等）保持一致

---

### 4. GUI 打开链路

**标准模式：**

```java
// BlockEntity 实现 MenuProvider
public class ShellBlockEntity extends BlockEntity implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.literal("外壳");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new ShellScreenHandler(syncId, inv, this);
    }
}

// Block 的 useWithoutItem 中打开
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
```

---

## 完整示例结构

```
ModBlocks.java          - 方块注册（使用 ResourceKey + Registry.register）
ModBlockEntities.java   - 方块实体注册（使用泛型 helper 方法）
CounterBlock.java       - 方块实现（正确实现 codec()）
CounterBlockEntity.java - 方块实体（使用 ValueInput/ValueOutput 序列化）
```

## 参考
- 官方示例位置：`26.1.2/src/main/java/com/example/docs/block/`
- 26.1.2 API 变更：包路径、注册表、ValueInput/ValueOutput 序列化系统
