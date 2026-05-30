---
name: minecraft-26-1-2-api-changes
description: Minecraft 26.1.2 (1.21+) 版本 API 变更对照表，用于修复模组代码
source: auto-skill
extracted_at: '2026-05-30T06:04:03.805Z'
---

# Minecraft 26.1.2 (1.21+) API 变更对照表

Mojang 在 26.1.2 版本中公布了官方混淆文件，许多 API 名称发生了变化。以下是主要的变更对照表。

## 包路径变更

| 旧路径 (1.20.1 及之前) | 新路径 (26.1.2+) |
|----------------------|-----------------|
| `net.minecraft.item.*` | `net.minecraft.world.item.*` |
| `net.minecraft.block.*` | `net.minecraft.world.level.block.*` |
| `net.minecraft.block.entity.*` | `net.minecraft.world.level.block.entity.*` |
| `net.minecraft.screen.*` | `net.minecraft.world.inventory.*` |
| `net.minecraft.entity.player.PlayerEntity` | `net.minecraft.world.entity.player.Player` |
| `net.minecraft.entity.player.PlayerInventory` | `net.minecraft.world.entity.player.Inventory` |
| `net.minecraft.text.Text` | `net.minecraft.network.chat.Component` |
| `net.minecraft.util.Identifier` | `net.minecraft.resources.Identifier` |
| `net.minecraft.registry.*` | `net.minecraft.core.registries.*` |
| `net.minecraft.nbt.*` | `net.minecraft.nbt.*` (类名变更) |
| `net.minecraft.client.gui.widget.*` | `net.minecraft.client.gui.components.*` |

## 类名变更

### NBT 相关
| 旧类名 | 新类名 |
|-------|-------|
| `NbtCompound` | `CompoundTag` |
| `NbtList` | `ListTag` |
| `NbtElement` | `Tag` |
| `NbtString` | `StringTag` |
| `NbtInt` | `IntTag` |
| `NbtDouble` | `DoubleTag` |
| `NbtBoolean` | `ByteTag` (布尔值用 byte 表示) |

### 其他核心类
| 旧类名 | 新类名 |
|-------|-------|
| `BlockWithEntity` | `BaseEntityBlock` |
| `BlockEntityProvider` | (移除，使用 `BaseEntityBlock` 即可) |
| `NamedScreenHandlerFactory` | `MenuProvider` |
| `ScreenHandler` | `AbstractContainerMenu` |
| `ScreenHandlerType` | `MenuType` |
| `SidedInventory` | (使用 `Container` 接口) |
| `DefaultedList` | (使用标准数组或 `List`) |
| `ClickableWidget` | `AbstractWidget` |
| `ButtonWidget` | `Button` |
| `HandledScreen` | `AbstractContainerScreen` |
| `CustomPayload` | `CustomPacketPayload` |
| `PacketCodec` | `StreamCodec` |
| `PacketCodecs` | `ByteBufCodecs` |
| `RegistryByteBuf` | `FriendlyByteBuf` |

## 方法签名变更

### CompoundTag 方法
| 旧方法 | 新方法 | 备注 |
|-------|-------|------|
| `getNbt("key")` | `getCompound("key")` | |
| `getList("key", type)` | `getList("key", typeId)` | type 使用 int 常量 |
| `getInt("key")` | `getInt("key")` | 返回 `Optional<Integer>` |
| `getBoolean("key")` | `getBoolean("key")` | 返回 `Optional<Boolean>` |
| `getString("key")` | `getString("key")` | 返回 `Optional<String>` |
| `getDouble("key")` | `getDouble("key")` | 返回 `Optional<Double>` |
| `getUuid("key")` | `getUUID("key")` | 注意大小写 |
| `putUuid("key", uuid)` | `putUUID("key", uuid)` | 注意大小写 |
| `contains("key", type)` | `contains("key", typeId)` | type 使用 int 常量 |

### Tag 类型常量
```java
// 旧写法
NbtElement.COMPOUND_TYPE
NbtElement.DOUBLE_TYPE

// 新写法 (使用 int 常量)
Tag.TAG_COMPOUND  // 10
Tag.DOUBLE        // 6 (不存在，直接用数字)
```

### ItemStack 方法
| 旧方法 | 新方法 | 备注 |
|-------|-------|------|
| `getNbt()` | `getTag()` / 使用 DataComponent 系统 | 26.1.2 推荐使用 DataComponent |
| `getOrCreateNbt()` | `getOrCreateTag()` | |
| `setNbt(nbt)` | `setTag(nbt)` | |

### Identifier 方法
| 旧方法 | 新方法 | 备注 |
|-------|-------|------|
| `Identifier.of(namespace, path)` | `Identifier.fromNamespaceAndPath(namespace, path)` | 直接返回 Identifier |
| `Identifier.tryBySeparator(':', namespace, path)` | `Identifier.fromNamespaceAndPath(namespace, path)` | 更简洁的写法 |

### 注册表方法
| 旧写法 | 新写法 | 备注 |
|-------|-------|------|
| `Registry.register(Registries.ITEM, id, item)` | `Registry.register(BuiltInRegistries.ITEM, id, item)` | 使用静态方法 |
| `BuiltInRegistries.ITEM.register(id, item)` | `Registry.register(BuiltInRegistries.ITEM, id, item)` | register 是静态方法 |
| `Registries.BLOCK` | `BuiltInRegistries.BLOCK` | |
| `Registries.BLOCK_ENTITY_TYPE` | `BuiltInRegistries.BLOCK_ENTITY_TYPE` | |
| `Registries.SCREEN_HANDLER` | `BuiltInRegistries.MENU` | |

### Block 相关
| 旧方法 | 新方法 | 备注 |
|-------|-------|------|
| `Block.Settings.create()` | `Block.Properties.of()` | |
| `Item.Settings` | `Item.Properties` | |
| `BlockState.getPlacementState(ctx)` | `BlockState.getStateForPlacement(ctx)` | |
| `StateManager.Builder` | `StateDefinition.Builder` | |
| `Properties.HORIZONTAL_FACING` | `BlockStateProperties.HORIZONTAL_FACING` | 类型为 `EnumProperty<Direction>` |
| `DirectionProperty` | `EnumProperty<Direction>` | |
| `BaseEntityBlock.codec()` | (需要实现) | 返回 `MapCodec<? extends BaseEntityBlock>`，抽象方法必须覆盖 |
| `BlockEntity.saveAdditional(nbt)` | `BlockEntity.saveAdditional(ValueOutput)` | 参数类型变化，使用 ValueInput/ValueOutput 替代 CompoundTag |
| `BlockEntity.loadAdditional(nbt)` | `BlockEntity.loadAdditional(ValueInput)` | 参数类型变化 |
| `BlockEntityType.Builder.create()` | `BlockEntityType.Builder.of()` | 26.1.2 中不可用，改用 Fabric API |
| `BlockEntityType.Builder.of()` | `FabricBlockEntityTypeBuilder.create()` | 使用 Fabric Object Builder API v1 |

### Screen/Menu 相关
| 旧方法 | 新方法 | 备注 |
|-------|-------|------|
| `ScreenHandler.createMenu()` | `AbstractContainerMenu.createMenu()` | |
| `ScreenHandler.quickMove()` | `AbstractContainerMenu.quickMoveStack()` | |
| `ScreenHandler.canUse()` | `AbstractContainerMenu.stillValid()` | |
| `ScreenHandler.addSlot()` | `AbstractContainerMenu.addSlot()` | |
| `ScreenHandler.slots` | `AbstractContainerMenu.slots` | |
| `Slot.canInsert()` | `Slot.mayPlace()` | |
| `Slot.getStack()` | `Slot.getItem()` | |
| `Slot.setStack()` | `Slot.set()` | |
| `Slot.markDirty()` | `Slot.setChanged()` | |
| `Inventories.writeNbt()` | `ContainerHelper.saveAllItems()` | |
| `Inventories.readNbt()` | `ContainerHelper.loadAllItems()` | |
| `ItemScatterer.spawn()` | `Containers.dropContents()` | |
| `ScreenHandlerType<T>` | `MenuType<T>` | 构造函数需要 `(MenuSupplier, FeatureFlagSet)` 两个参数 |
| `new MenuType<>(factory)` | `new MenuType<>(factory, FeatureFlags.VANILLA_SET)` | 必须提供 FeatureFlagSet |

### 客户端 GUI
| 旧方法 | 新方法 | 备注 |
|-------|-------|------|
| `HandledScreen.drawBackground()` | `AbstractContainerScreen.renderBg()` | |
| `HandledScreen.drawForeground()` | `AbstractContainerScreen.renderLabels()` | |
| `HandledScreen.drawMouseoverTooltip()` | `AbstractContainerScreen.renderTooltip()` | |
| `Screen.addDrawableChild()` | `Screen.addRenderableWidget()` | |
| `ButtonWidget.builder().position().size()` | `Button.builder().bounds()` | |
| `ClickableWidget.renderWidget()` | `AbstractWidget.renderWidget()` | 26.1.2 中改为 `extractWidgetRenderState(GuiGraphicsExtractor, ...)` |
| `ClickableWidget.appendClickableNarrations()` | `AbstractWidget.updateWidgetNarration()` | |
| `DrawContext.drawBorder()` | `DrawContext.renderOutline()` | |
| `DrawContext.drawText()` | `DrawContext.drawString()` | |
| `DrawContext.drawTexture()` | `DrawContext.blit()` | |
| `GuiGraphics` | `GuiGraphicsExtractor` | AbstractWidget 渲染方法使用的新类型 |
| `AbstractWidget.renderWidget()` | `AbstractWidget.extractWidgetRenderState()` | 26.1.2 中抽象方法签名变更 |

### 网络包
| 旧写法 | 新写法 | 备注 |
|-------|-------|------|
| `CustomPayload.Id` | `CustomPacketPayload.Type` | |
| `PacketCodec.tuple()` | `StreamCodec.composite()` | |
| `PacketCodecs.INTEGER` | `ByteBufCodecs.INT` | |
| `PacketCodecs.BYTE_ARRAY` | `ByteBufCodecs.BYTE_ARRAY` | |
| `PacketCodecs.STRING` | `ByteBufCodecs.STRING_UTF8` | |
| `PayloadTypeRegistry.playC2S()` | (Fabric API 变化) | 26.1.2 中可能已移除或重命名 |
| `PayloadTypeRegistry.playS2C()` | (Fabric API 变化) | 26.1.2 中可能已移除或重命名 |
| `ServerPlayNetworking.send()` | `ServerPlayNetworking.send()` | 保持不变 |

### 实体/玩家
| 旧方法 | 新方法 | 备注 |
|-------|-------|------|
| `player.getServer()` | `player.level.getServer()` | |
| `player.openHandledScreen()` | `player.openMenu()` | |
| `player.sendMessage()` | `player.displayClientMessage()` | |
| `world.isClient` | `level.isClientSide()` | 现在是方法 |
| `BlockEntity.markDirty()` | `BlockEntity.setChanged()` | |
| `BlockEntity.readNbt()` | `BlockEntity.loadAdditional()` | |
| `BlockEntity.writeNbt()` | `BlockEntity.saveAdditional()` | |
| `BlockEntity.toTag()` | (移除) | 使用 `saveWithFullMetadata()` |
| `BlockEntity.fromTag()` | (移除) | 使用 `loadWithComponents()` |

## 代码示例

### 物品注册
```java
// 旧写法
Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "item"), item);

// 新写法
Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "item");
Registry.register(BuiltInRegistries.ITEM, id, item);
```

### NBT 读写
```java
// 旧写法
NbtCompound nbt = stack.getNbt();
int value = nbt.getInt("key");

// 新写法
CompoundTag nbt = stack.getTag();
int value = nbt.getInt("key").orElse(0); // 处理 Optional
```

### Block 定义
```java
// 旧写法
public class MyBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public MyBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }
}

// 新写法
public class MyBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = ...; // 需查找新定义

    public MyBlock(Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }
    
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // 必须实现，返回 MapCodec
        return null; // 或实现正确的 codec
    }
}
```

### BlockEntityType 注册（使用 Fabric API）
```java
// 26.1.2 中 BlockEntityType.Builder 不可用，改用 Fabric API
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

public static final BlockEntityType<MyBlockEntity> MY_BLOCK_ENTITY;

static {
    MY_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(MyBlockEntity::new, MY_BLOCK).build(null);
    Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, 
        Identifier.fromNamespaceAndPath(MOD_ID, "my_block_entity"), MY_BLOCK_ENTITY);
}
```

### MenuType 注册
```java
// 旧写法
public static final ScreenHandlerType<MyScreenHandler> MY_HANDLER;
static {
    MY_HANDLER = Registry.register(Registries.SCREEN_HANDLER, id, new ScreenHandlerType<>(MyScreenHandler::new));
}

// 新写法 (26.1.2)
public static final MenuType<MyMenu> MY_MENU;
static {
    MY_MENU = new MenuType<>((syncId, inventory) -> new MyMenu(syncId, inventory), FeatureFlags.VANILLA_SET);
    Registry.register(BuiltInRegistries.MENU, id, MY_MENU);
}
```

### ScreenHandler
```java
// 旧写法
public class MyScreenHandler extends ScreenHandler {
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) { ... }
    @Override
    public boolean canUse(PlayerEntity player) { ... }
}

// 新写法
public class MyMenu extends AbstractContainerMenu {
    @Override
    public ItemStack quickMoveStack(Player player, int index) { ... }
    @Override
    public boolean stillValid(Player player) { ... }
}
```

### AbstractWidget (26.1.2)
```java
// 旧写法
public class MyWidget extends AbstractWidget {
    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX(), getY(), getX() + width, getY() + height, 0xFF404040);
    }
}

// 新写法 (26.1.2)
public class MyWidget extends AbstractWidget {
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // 渲染逻辑，使用 GuiGraphicsExtractor 而非 GuiGraphics
    }
}
```

## 验证步骤

修复完成后，运行以下命令验证：

```bash
./gradlew build
```

常见错误及解决方案：
1. `找不到符号` - 检查包路径和类名是否已更新
2. `不兼容的类型：Optional<X>无法转换为X` - 使用 `.orElse(default)` 或 `.get()` 处理 Optional
3. `方法不会覆盖或实现超类型的方法` - 检查方法签名是否已更新
