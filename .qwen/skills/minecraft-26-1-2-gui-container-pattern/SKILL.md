---
name: minecraft-26-1-2-gui-container-pattern
description: Minecraft 26.1.2 GUI 系统架构模式 - Container 接口适配与共享包结构
source: auto-skill
extracted_at: '2026-05-31T05:30:00.000Z'
---

# Minecraft 26.1.2 GUI 系统架构模式

在 26.1.2 版本中，GUI 系统的架构需要遵循特定的包结构和接口实现模式。本文档记录了实现自定义容器 GUI 的最佳实践。

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
    private final ShellContainer container;

    public ShellScreenHandler(int syncId, Inventory playerInventory, ShellContainer container) {
        super(RelayScreenHandlers.SHELL_SCREEN_HANDLER, syncId);
        this.container = container;

        // 容器插槽 - 使用 ShellContainerWrapper 适配
        for (int i = 0; i < 4; ++i) {
            final int slotIndex = i;
            this.addSlot(new Slot(new ShellContainerWrapper(container), slotIndex, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true; // 或实现物品过滤
                }
            });
        }

        // 玩家物品栏
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 140 + y * 18));
            }
        }

        // 玩家热键栏
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 198));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        return ItemStack.EMPTY; // 实现快速移动逻辑
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
```

### 4. MenuType 注册模式

```java
public class RelayScreenHandlers {
    public static final MenuType<ShellScreenHandler> SHELL_SCREEN_HANDLER;

    static {
        // MenuType 构造函数需要 (MenuSupplier, FeatureFlagSet)
        SHELL_SCREEN_HANDLER = new MenuType<>(
            (syncId, inventory) -> new ShellScreenHandler(syncId, inventory),
            FeatureFlags.VANILLA_SET
        );
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, "shell");
        Registry.register(BuiltInRegistries.MENU, id, SHELL_SCREEN_HANDLER);
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
        qdream.relay.screen.RelayScreenHandlers.init();
    }
}
```

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
