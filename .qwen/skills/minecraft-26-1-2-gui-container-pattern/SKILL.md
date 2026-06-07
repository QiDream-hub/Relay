---
name: minecraft-26-1-2-gui-container-pattern
description: Minecraft 26.1.2 GUI 系统架构模式 - Container 接口适配、MenuScreens 注册与泛型
source: auto-skill
extracted_at: '2026-06-06T03:30:00.000Z'
---

# Minecraft 26.1.2 GUI 系统架构模式

在 26.1.2 版本中，GUI 系统的架构需要遵循特定的包结构和接口实现模式。本文档记录了实现自定义容器 GUI 的最佳实践。

## 完整 GUI 标准链路

```
玩家右键方块
    ↓
Block.useWithoutItem()
    ↓
world.getBlockEntity(pos) 获取 BlockEntity
    ↓
player.openMenu((MenuProvider) blockEntity)
    ↓
BlockEntity.createMenu() → 返回 AbstractContainerMenu
    ↓
客户端通过 MenuScreens 获取 Screen 工厂
    ↓
创建 AbstractContainerScreen<T>
```

**核心要点**：
- 方块不需要直接声明 GUI
- `BlockEntity` 实现 `MenuProvider` 接口
- 右键时调用 `player.openMenu()` 触发 GUI 打开
- 客户端必须通过 `MenuScreens.register()` 注册 Screen 工厂
- `AbstractContainerScreen` 是泛型类，必须指定类型参数

## 核心问题

1. **包可见性问题**: `AbstractContainerMenu` 和 `Slot` 在服务端和客户端都需要访问，不能放在客户端包中
2. **Container 接口要求**: `Slot` 构造函数需要 `Container` 接口类型，自定义容器类必须实现该接口
3. **接口分离**: 业务逻辑接口 (`ShellContainer`) 和 GUI 接口 (`Container`) 需要分离，使用包装器适配

## 解决方案

### 1. 包结构设计

```
src/main/java/
├── qdream/relay/
│   ├── screen/              # 共享 GUI 包（服务端 + 客户端都可访问）
│   │   ├── ShellScreenHandler.java    # 容器菜单
│   │   └── RelayScreenHandlers.java   # 菜单类型注册
│   ├── items/
│   │   └── ShellContainerWrapper.java # Container 接口包装器
│   └── core/
│       └── ShellContainer.java        # 业务逻辑接口
```

**关键规则**:
- `ShellScreenHandler` 必须在共享包（`src/main/java`），不能在客户端包（`src/client/java`）
- `RelayScreenHandlers` 注册表也必须在共享包
- 客户端初始化器从共享包导入并调用 `init()`

### 2. Container 接口实现模式

使用包装器类将业务逻辑接口适配到 `Container` 接口：

```java
public class ShellContainerWrapper implements Container {
    private final ShellContainer delegate;

    public ShellContainerWrapper(ShellContainer delegate) {
        this.delegate = delegate;
    }

    // Container 接口方法 - 固定模板
    @Override
    public int getContainerSize() {
        return 4; // 插槽数量
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return delegate.getInventorySlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (!stack.isEmpty()) {
            ItemStack result = stack.split(amount);
            setItem(slot, stack);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getItem(slot);
        if (!stack.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        delegate.setInventorySlot(slot, stack);
    }

    @Override
    public void setChanged() {
        delegate.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // 或实现距离检查
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
    }
}
```

### 3. ScreenHandler 实现模式

```java
public class ShellScreenHandler extends AbstractContainerMenu {
    // 插槽布局常量
    private static final int CONTAINER_SLOT_COUNT = 4;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int PLAYER_HOTBAR_SLOT_COUNT = 9;

    // 插槽索引范围
    private static final int CONTAINER_START = 0;
    private static final int CONTAINER_END = CONTAINER_SLOT_COUNT;
    private static final int INVENTORY_START = CONTAINER_END;
    private static final int INVENTORY_END = INVENTORY_START + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int HOTBAR_START = INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + PLAYER_HOTBAR_SLOT_COUNT;

    // GUI 布局
    private static final int CONTAINER_SLOT_X = 80;
    private static final int CONTAINER_SLOT_Y = 10;
    private static final int SLOT_SPACING_Y = 30;
    private static final int INVENTORY_SLOT_X = 8;
    private static final int INVENTORY_SLOT_Y = 140;
    private static final int HOTBAR_SLOT_Y = 198;
    private static final int SLOT_SIZE = 18;

    private final ShellContainer container;
    private final Container wrapper;

    /**
     * 客户端构造方法（没有实际容器）
     */
    public ShellScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    /**
     * 服务端构造方法（有实际容器）
     */
    public ShellScreenHandler(int syncId, Inventory playerInventory, ShellContainer container) {
        super(RelayScreenHandlers.SHELL_SCREEN_HANDLER, syncId);
        this.container = container;
        this.wrapper = container != null ? new ShellContainerWrapper(container) : new EmptyShellContainer();

        checkContainerSize(this.wrapper, CONTAINER_SLOT_COUNT);

        // 容器插槽 - 使用 ShellContainerWrapper 适配
        for (int i = 0; i < CONTAINER_SLOT_COUNT; ++i) {
            final int slotIndex = i;
            this.addSlot(new Slot(this.wrapper, slotIndex, CONTAINER_SLOT_X, CONTAINER_SLOT_Y + i * SLOT_SPACING_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true; // 或实现物品过滤
                }
            });
        }

        // 玩家物品栏
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, INVENTORY_SLOT_X + x * SLOT_SIZE, INVENTORY_SLOT_Y + y * SLOT_SIZE));
            }
        }

        // 玩家热键栏
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, INVENTORY_SLOT_X + x * SLOT_SIZE, HOTBAR_SLOT_Y));
        }
    }

    /**
     * Shift+点击物品时的快速移动逻辑
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            clicked = stackInSlot.copy();

            // 从容器插槽移动到玩家物品栏
            if (slotIndex < CONTAINER_END) {
                if (!this.moveItemStackTo(stackInSlot, INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家物品栏移动到容器插槽
            else if (slotIndex < HOTBAR_END) {
                if (!this.moveItemStackTo(stackInSlot, CONTAINER_START, CONTAINER_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            // 更新插槽状态
            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.wrapper.stillValid(player);
    }

    /**
     * 空容器实现（用于客户端）
     */
    private static class EmptyShellContainer extends ShellContainerWrapper {
        private final ItemStack[] emptyInventory = new ItemStack[CONTAINER_SLOT_COUNT];

        public EmptyShellContainer() {
            super(new DummyShellContainer());
            for (int i = 0; i < CONTAINER_SLOT_COUNT; i++) {
                emptyInventory[i] = ItemStack.EMPTY;
            }
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < CONTAINER_SLOT_COUNT ? emptyInventory[slot] : ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot >= 0 && slot < CONTAINER_SLOT_COUNT) {
                emptyInventory[slot] = stack;
            }
        }
    }

    /**
     * 伪容器用于 EmptyShellContainer 的构造
     */
    private static class DummyShellContainer implements ShellContainer {
        @Override public ItemStack getInventorySlot(int slot) { return ItemStack.EMPTY; }
        @Override public void setInventorySlot(int slot, ItemStack stack) {}
        @Override public StateMachine getStateMachine() { return new StateMachine(1024); }
        @Override public int getCoreCount() { return 0; }
        @Override public int getInterval() { return 1; }
        @Override public boolean isInitialized() { return false; }
        @Override public void setInitialized(boolean initialized) {}
        @Override public int getEnergy() { return 0; }
        @Override public void setEnergy(int energy) {}
        @Override public void setChanged() {}
        @Override public boolean isClientSide() { return true; }
    }
}
```

**关键要点**:
- **双构造方法模式**: 客户端构造方法调用服务端构造方法并传入 `null` 容器
- **EmptyShellContainer**: 客户端需要空容器实现，避免 `NullPointerException`
- **插槽索引范围常量**: 便于 `quickMoveStack` 中判断物品来源
- **quickMoveStack 逻辑**: 
  - 从容器到物品栏：`moveItemStackTo(stack, INVENTORY_START, HOTBAR_END, true)`（从后往前填）
  - 从物品栏到容器：`moveItemStackTo(stack, CONTAINER_START, CONTAINER_END, false)`（从前往后填）
- **stillValid 委托**: 委托给 wrapper 检查，客户端空容器始终返回 `true`

### 4. MenuType 注册模式

```java
public class RelayScreenHandlers {
    public static final MenuType<ShellScreenHandler> SHELL_SCREEN_HANDLER;
    public static final MenuType<SpellEditorScreenHandler> SPELL_EDITOR_SCREEN_HANDLER;

    static {
        // MenuType 构造函数需要 (MenuSupplier, FeatureFlagSet)
        SHELL_SCREEN_HANDLER = new MenuType<>(
            (syncId, inventory) -> new ShellScreenHandler(syncId, inventory),
            FeatureFlags.VANILLA_SET
        );
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "shell");
        Registry.register(BuiltInRegistries.MENU, id, SHELL_SCREEN_HANDLER);

        SPELL_EDITOR_SCREEN_HANDLER = new MenuType<>(
            (syncId, inventory) -> new SpellEditorScreenHandler(syncId, inventory),
            FeatureFlags.VANILLA_SET
        );
        Identifier editorId = Identifier.fromNamespaceAndPath(MOD_ID, "spell_editor");
        Registry.register(BuiltInRegistries.MENU, editorId, SPELL_EDITOR_SCREEN_HANDLER);
    }

    public static void init() {}
}
```

### 5. MenuProvider 实现模式

在方块实体或实体中实现 `MenuProvider` 接口：

```java
public class ShellBlockEntity extends BlockEntity implements MenuProvider, ShellContainer {
    // ...

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

### 6. 客户端初始化

```java
public class RelayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 从共享包导入并初始化
        RelayScreenHandlersClient.init();
    }
}
```

### 7. MenuScreens 注册（关键！）

**必须**在客户端注册 Screen 工厂，否则打开 GUI 时会报错 `Failed to create screen for menu type`：

```java
public class RelayScreenHandlersClient {
    public static void init() {
        // 注册外壳方块屏幕
        MenuScreens.register(RelayScreenHandlers.SHELL_SCREEN_HANDLER, ShellScreen::new);

        // 注册法术编辑器屏幕
        MenuScreens.register(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, SpellEditorScreen::new);
    }
}
```

### 8. AbstractContainerScreen 实现模式

```java
public class ShellScreen extends AbstractContainerScreen<ShellScreenHandler> {

    private static final Identifier CONTAINER_TEXTURE = 
        Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/container/shell.png");

    public ShellScreen(ShellScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        // 标题居中
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        // 玩家物品栏标题位置
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        // 渲染容器纹理背景
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CONTAINER_TEXTURE,
            this.leftPos,
            this.topPos,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            BACKGROUND_TEXTURE_WIDTH,
            BACKGROUND_TEXTURE_HEIGHT
        );
    }
}
```

**关键要点**:
- **泛型参数**: `AbstractContainerScreen<T>` 的 `T` 必须与 `MenuType<T>` 的类型一致
- **extractBackground**: 26.1.2 使用此方法渲染背景，而非旧的 `renderBackground`
- **extractRenderState**: 用于渲染自定义 UI 内容（文本、分隔线等）
- **纹理路径**: 使用 `Identifier.fromNamespaceAndPath(modId, path)` 指定纹理
- **标题居中**: `titleLabelX = (imageWidth - font.width(title)) / 2`

## 常见错误及解决方案

### 错误 1: `找不到符号 - ShellScreenHandler`
**原因**: `ShellScreenHandler` 在客户端包中，服务端代码无法访问  
**解决**: 将 `ShellScreenHandler` 移到 `src/main/java` 的共享包

### 错误 2: `不兼容的类型: X 无法转换为 Container`
**原因**: `Slot` 构造函数需要 `Container` 接口类型  
**解决**: 创建包装器类实现 `Container` 接口，委托调用业务逻辑

### 错误 3: `类 X 不是抽象的，未覆盖 clearContent()`
**原因**: `Container` 接口继承自 `Clearable`，必须实现 `clearContent()`  
**解决**: 添加 `clearContent()` 方法实现

### 错误 4: `MenuType 构造函数需要两个参数`
**原因**: 26.1.2 的 `MenuType` 需要 `(MenuSupplier, FeatureFlagSet)`
**解决**: 使用 `new MenuType<>(factory, FeatureFlags.VANILLA_SET)`

### 错误 5: `Failed to create screen for menu type: modid:menu_id`
**原因**: 客户端没有通过 `MenuScreens.register()` 注册 Screen 工厂
**解决**: 在 `ClientModInitializer.onInitializeClient()` 中注册：
```java
MenuScreens.register(RelayScreenHandlers.MENU_TYPE, MyScreen::new);
```

### 错误 6: `不兼容的类型：无法推断 ScreenConstructor 的函数接口描述符`
**原因**: `AbstractContainerScreen` 缺少泛型参数类型
**解决**: 继承时指定类型参数：
```java
public class MyScreen extends AbstractContainerScreen<MyMenuType> { ... }
```

### 错误 7: `类型参数 X 不在类型变量 U 的范围内`
**原因**: `ScreenConstructor` 要求 `U extends Screen, MenuAccess<T>`
**解决**: 确保 `AbstractContainerScreen` 继承时指定正确的泛型参数，它已实现 `MenuAccess<T>`

### 错误 8: `无法为 final 变量 imageWidth 分配值`
**原因**: 26.1.2 中 `AbstractContainerScreen` 的 `imageWidth` 和 `imageHeight` 是 final 字段
**解决**: 使用带尺寸参数的构造函数：
```java
public MyScreen(MyMenuHandler handler, Inventory inventory, Component title) {
    super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
}
```

### 错误 9: GUI 背景不显示，面板内容为空白
**原因**: `AbstractContainerScreen` 不会自动渲染背景纹理，需要手动绘制
**解决**: 在 `extractRenderState()` 中手动绘制背景和边框：
```java
@Override
public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    // 1. 绘制深色背景
    graphics.fill(this.leftPos, this.topPos, 
                  this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 
                  0xFF101010);
    
    // 2. 绘制边框
    graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF404040);
    
    // 3. 调用父类渲染 Slot 等
    super.extractRenderState(graphics, mouseX, mouseY, delta);
    
    // 4. 绘制分隔线（可选）
    graphics.verticalLine(left + PANEL_WIDTH, top, top + this.imageHeight, 0xFF404040);
    
    // 5. 绘制自定义内容
    graphics.text(this.font, "标题", left + 5, top + 5, 0x00FF00);
}
```

**常用渲染方法**:
- `graphics.fill(x0, y0, x1, y1, color)` - 填充矩形
- `graphics.outline(x, y, width, height, color)` - 绘制边框
- `graphics.verticalLine(x, y0, y1, color)` - 垂直线
- `graphics.horizontalLine(x0, x1, y, color)` - 水平线
- `graphics.text(font, text, x, y, color)` - 渲染文本

## 接口关系图

```
┌─────────────────────┐
│  ShellContainer     │ ← 业务逻辑接口（自定义）
│  - getInventorySlot │
│  - setInventorySlot │
│  - getStateMachine  │
└─────────┬───────────┘
          │ 实现
          ▼
┌─────────────────────┐
│ ShellBlockEntity    │ ← 方块实体
│ EntityShell         │ ← 实体
│ ToolShellContainer  │ ← 物品容器
└─────────┬───────────┘
          │ 委托
          ▼
┌─────────────────────┐
│ShellContainerWrapper│ ← 包装器（实现 Container）
│ - implements Container
│ - delegate: ShellContainer
└─────────┬───────────┘
          │ 用于
          ▼
┌─────────────────────┐
│  Slot               │ ← Minecraft GUI 插槽
│  Slot(new ShellContainerWrapper(...))
└─────────────────────┘
```

## 验证步骤

```bash
./gradlew build
```

成功标志：
- 无编译错误
- `BUILD SUCCESSFUL`
- 服务端和客户端都能正常编译
