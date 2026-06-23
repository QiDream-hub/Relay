---
name: relay-spell-editor-gui-rendering
description: Relay 法术编辑器 GUI 渲染与布局适配 - 26.1.2 AbstractContainerScreen 背景渲染、官方纹理使用、按钮定位、面板布局
source: auto-skill
extracted_at: '2026-06-24T12:00:00.000Z'
---

# Relay 法术编辑器 GUI 渲染与布局适配

在 Minecraft 26.1.2 中使用 `AbstractContainerScreen` 实现自定义 GUI 的完整渲染流程，包括背景绘制、官方纹理使用、按钮定位、面板布局调整。

## 方案选择

### 方案 1: 使用官方纹理（推荐）
仿照熔炉等原版 GUI，使用 Minecraft 官方纹理作为背景，自动渲染玩家背包。

**优点**：
- 与原版 GUI 风格一致
- 玩家背包自动渲染，无需手动处理
- 纹理经过官方设计，视觉效果更好

**缺点**：
- 尺寸固定为 176x166（标准熔炉尺寸）
- 布局需要适应纹理的插槽位置

### 方案 2: 自定义纯色背景
手动绘制纯色背景和边框，完全自定义布局。

**优点**：
- 尺寸和布局完全自由
- 适合复杂的多栏布局

**缺点**：
- 需要手动渲染背包背景
- 视觉风格可能与原版不一致

## 使用官方纹理的实现方法

### 1. 定义纹理 Identifier

```java
private static final Identifier BACKGROUND_TEXTURE = 
    Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
```

### 2. 设置标准尺寸

```java
private static final int GUI_WIDTH = 176;   // 标准熔炉宽度
private static final int GUI_HEIGHT = 166;  // 标准熔炉高度

public SpellEditorScreen(SpellEditorScreenHandler handler, Inventory inventory, Component title) {
    super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
    this.inventoryLabelY = 84;  // 背包标签 Y 位置（熔炉标准）
}
```

### 3. 渲染背景纹理

```java
@Override
public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    // 渲染官方背景纹理
    int x = this.leftPos;
    int y = this.topPos;
    graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, 
                  x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);

    // 调用父类渲染 Slot（物品栏）
    super.extractRenderState(graphics, mouseX, mouseY, delta);

    // 渲染自定义内容（标题、按钮等）
    graphics.text(this.font, "操作", x + 8, y + 8, 0xFF00FF00);
    graphics.text(this.font, "JSON", x + 68, y + 8, 0xFFFFFF00);
}
```

### 4. ScreenHandler 插槽位置对齐

熔炉的背包位置在 `(8, 84)`，需要调整 ScreenHandler 中的插槽坐标：

```java
// SpellEditorScreenHandler.java
private static final int INVENTORY_START_X = 8;   // 熔炉背包 X 位置
private static final int INVENTORY_START_Y = 84;  // 熔炉背包 Y 位置
private static final int SLOT_SIZE = 18;

public SpellEditorScreenHandler(int syncId, Inventory playerInventory, ...) {
    // 玩家主物品栏（3 行 x 9 列）
    for (int y = 0; y < 3; ++y) {
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x + y * 9 + 9,
                INVENTORY_START_X + x * SLOT_SIZE,
                INVENTORY_START_Y + y * SLOT_SIZE));
        }
    }

    // 玩家热键栏（1 行 x 9 列）
    int hotbarY = INVENTORY_START_Y + 54;
    for (int x = 0; x < 9; ++x) {
        this.addSlot(new Slot(playerInventory, x,
            INVENTORY_START_X + x * SLOT_SIZE,
            hotbarY));
    }
}
```

## 核心问题与解决方案（自定义背景方案）

### 问题 1: 背景不显示
**现象**: GUI 打开后透明，看不到面板背景

**原因**: `AbstractContainerScreen` 不会自动渲染背景纹理，需要手动绘制

**解决方案**: 在 `extractRenderState` 中使用 `graphics.fill()` 绘制纯色背景
```java
@Override
public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    // 绘制深色背景
    graphics.fill(this.leftPos, this.topPos,
                 this.leftPos + this.imageWidth,
                 this.topPos + this.imageHeight,
                 0xFF101010);

    // 绘制边框
    graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF404040);

    // 调用父类渲染 Slot（物品栏）
    super.extractRenderState(graphics, mouseX, mouseY, delta);

    // ... 渲染其他内容
}
```

### 问题 2: 按钮与背景重叠
**现象**: 按钮位置超出背景区域，显示在 GUI 外部

**原因**: 按钮 Y 坐标计算错误，超过了 `imageHeight`

**解决方案**: 
1. 增加 GUI 高度容纳所有元素
2. 按钮位置基于 `leftPos/topPos` + 面板偏移计算
```java
// 布局常量
private static final int GUI_WIDTH = 400;
private static final int GUI_HEIGHT = 280;  // 增加高度

public SpellEditorScreen(...) {
    super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
}

@Override
protected void init() {
    // 按钮在右侧面板区域，Y 坐标确保 < imageHeight
    int buttonX = this.leftPos + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING * 3;
    int buttonY = this.topPos + 180;  // 180 + 50 + 20 = 250 < 280
    
    this.addRenderableWidget(Button.builder(...)
        .pos(buttonX, buttonY).size(60, 20).build());
}
```

### 问题 3: 构造函数尺寸传递
**现象**: 编译错误 "无法为 final 变量 imageWidth 分配值"

**原因**: 26.1.2 中 `imageWidth` 和 `imageHeight` 是 final 字段

**解决方案**: 通过构造函数传递尺寸，而非直接赋值
```java
// ❌ 错误写法
public SpellEditorScreen(...) {
    super(handler, inventory, title);
    this.imageWidth = 400;  // 编译错误
}

// ✅ 正确写法
public SpellEditorScreen(...) {
    super(handler, inventory, title, 400, 280);  // 通过构造函数传递
}
```

## 完整布局设计

### 三栏布局结构
```
┌─────────────────────────────────────────────────────────┐
│ 可用操作 (120px) │ 程序列表 (120px) │ 栈视图 (160px)    │
│                 │                 │                    │
│ push            │ 1. push         │ 数据栈:            │
│ pop             │ 2. add          │   NUMBER: 5        │
│ add             │ 3. dup          │   NUMBER: 3        │
│ ...             │ ...             │                    │
│                 │                 │ 程序栈:            │
│                 │                 │   OPER: add        │
│                 │                 │                    │
│ 共 24 个操作     │                 │ [运行] [清空] [删除]│
└─────────────────────────────────────────────────────────┘
```

### 布局常量定义
```java
private static final int OPERATIONS_PANEL_WIDTH = 120;   // 左栏宽度
private static final int PROGRAM_PANEL_WIDTH = 120;      // 中栏宽度
private static final int PANEL_PADDING = 10;             // 面板间距
private static final int LINE_HEIGHT = 12;               // 行高
private static final int GUI_WIDTH = 400;                // GUI 总宽度
private static final int GUI_HEIGHT = 280;               // GUI 总高度

// 面板 X 坐标（相对于 leftPos）
private static final int OPERATIONS_X = 0;
private static final int PROGRAM_X = OPERATIONS_PANEL_WIDTH;
private static final int STACK_X = OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH;
```

### 分隔线渲染
```java
// 垂直分隔线
graphics.verticalLine(left + OPERATIONS_PANEL_WIDTH, top, top + imageHeight, 0xFF404040);
graphics.verticalLine(left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH, top, top + imageHeight, 0xFF404040);
```

## 物品栏位置调整

物品栏插槽位置必须与 GUI 高度匹配：

```java
public SpellEditorScreenHandler(int syncId, Inventory playerInventory, ...) {
    // 玩家主物品栏（3 行）
    for (int y = 0; y < 3; ++y) {
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 
                                  8 + x * 18, 170 + y * 18));  // Y 从 170 开始
        }
    }
    
    // 玩家热键栏（1 行）
    for (int x = 0; x < 9; ++x) {
        this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 248));  // Y = 248
    }
}
```

## 渲染顺序

正确的渲染顺序确保视觉层次正确：

```java
@Override
public void extractRenderState(GuiGraphicsExtractor graphics, ...) {
    // 1. 背景（最底层）
    graphics.fill(...);
    graphics.outline(...);
    
    // 2. 父类 Slot 渲染
    super.extractRenderState(graphics, ...);
    
    // 3. 分隔线
    graphics.verticalLine(...);
    
    // 4. 面板标题
    graphics.text(..., "可用操作", ...);
    graphics.text(..., "程序列表", ...);
    graphics.text(..., "栈视图", ...);
    
    // 5. 面板内容
    renderOperationList(...);
    renderProgramList(...);
    renderStacks(...);
    
    // 6. 提示信息（最上层）
    graphics.text(..., "点击操作添加", ...);
    graphics.text(..., "事故：...", ...);
}
```

## 颜色方案

使用十六进制 ARGB 颜色值：

```java
// 背景
0xFF101010  // 深灰背景
0xFF404040  // 边框/分隔线

// 文本
0x00FF00    // 可用操作（绿色）
0xFFFF00    // 程序列表标题（黄色）
0x00FFFF    // 栈视图标题（青色）
0x00AA00    // 操作项（深绿）
0xFFFFFF    // 程序项（白色）
0xFF0000    // 事故信息（红色）
0x888888    // 提示文本（灰色）
0x666666    // 次要文本（深灰）

// 高亮
0x40FFFF00  // 选中背景（半透明黄色）
```

## 调试技巧

### 1. 添加调试日志
```java
public SpellEditorScreenHandler(...) {
    System.out.println("[SpellEditorScreenHandler] 可用操作数量：" + availableOperations.size());
    for (String op : availableOperations) {
        System.out.println("  - " + op);
    }
}
```

### 2. 验证布局边界
```java
// 在渲染时检查坐标是否超出范围
int buttonBottom = buttonY + 20;
if (buttonBottom > this.topPos + this.imageHeight) {
    System.out.println("警告：按钮超出 GUI 底部！");
}
```

## 物品栏面板 Widget 化

将玩家背包单独渲染为一个独立的 Widget，避免背景渲染超出背包范围，并确保居中显示。

### 问题背景
- 默认的物品栏渲染由 `AbstractContainerScreen` 管理，背景由 ScreenHandler 的插槽控制
- 直接渲染物品栏背景时容易出现背景超出、不居中的问题
- 需要将视觉渲染（Widget）与插槽逻辑（ScreenHandler）分离

### 解决方案：InventoryPanelWidget

创建独立的背包面板 Widget，只渲染背景和插槽框，不依赖 Inventory 对象：

```java
// InventoryPanelWidget.java
public class InventoryPanelWidget extends AbstractWidget {
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 18;
    private static final int MAIN_INVENTORY_ROWS = 3;
    private static final int HOTBAR_ROW_Y_OFFSET = 54;
    private static final int PADDING = 8;

    public InventoryPanelWidget(int x, int y, int width, int height, Font font) {
        super(x, y, width, height, Component.empty());
        this.font = font;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, ...) {
        int x = getX();
        int y = getY();

        // 渲染背景
        graphics.fill(x, y, x + this.width, y + this.height, BG_COLOR);
        graphics.outline(x, y, this.width, this.height, BORDER_COLOR);

        // 计算物品栏起始位置（标题下方）
        int invStartX = x + PADDING;
        int invStartY = y + PADDING + 10;

        // 渲染主物品栏（3 行 x 9 列）
        for (int row = 0; row < MAIN_INVENTORY_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = invStartX + col * SLOT_SPACING;
                int slotY = invStartY + row * SLOT_SPACING;
                renderSlotBackground(graphics, slotX, slotY);
            }
        }

        // 渲染热键栏（1 行 x 9 列）
        int hotbarY = invStartY + HOTBAR_ROW_Y_OFFSET;
        for (int col = 0; col < 9; col++) {
            int slotX = invStartX + col * SLOT_SPACING;
            renderSlotBackground(graphics, slotX, hotbarY);
        }
    }

    private void renderSlotBackground(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BG_COLOR);
        graphics.outline(x, y, SLOT_SIZE, SLOT_SIZE, SLOT_BORDER_COLOR);
    }
}
```

### ScreenHandler 插槽位置对齐

确保 ScreenHandler 中的插槽位置与 Widget 渲染的插槽背景一致：

```java
// SpellEditorScreenHandler.java
private static final int INVENTORY_START_X = 124; // (410 - 178) / 2 + 8
private static final int INVENTORY_START_Y = 310;
private static final int SLOT_SIZE = 18;

public SpellEditorScreenHandler(int syncId, Inventory playerInventory, ...) {
    // 玩家主物品栏（3 行 x 9 列）
    for (int y = 0; y < 3; ++y) {
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x + y * 9 + 9,
                INVENTORY_START_X + x * SLOT_SIZE,
                INVENTORY_START_Y + y * SLOT_SIZE));
        }
    }

    // 玩家热键栏（1 行 x 9 列）
    int hotbarY = INVENTORY_START_Y + 54;
    for (int x = 0; x < 9; ++x) {
        this.addSlot(new Slot(playerInventory, x,
            INVENTORY_START_X + x * SLOT_SIZE,
            hotbarY));
    }
}
```

### Screen 中添加 Widget

在 `SpellEditorScreen.init()` 中添加背包面板 Widget，居中放置：

```java
// SpellEditorScreen.java
private static final int INVENTORY_PANEL_WIDTH = 178;
private static final int INVENTORY_PANEL_HEIGHT = 100;

@Override
protected void init() {
    // ... 其他初始化 ...

    // 背包面板 Widget（居中放置在底部）
    int inventoryPanelX = left + (this.imageWidth - INVENTORY_PANEL_WIDTH) / 2;
    int inventoryPanelY = top + EDITOR_PANEL_HEIGHT + 10;
    inventoryPanelWidget = new InventoryPanelWidget(
        inventoryPanelX, inventoryPanelY,
        INVENTORY_PANEL_WIDTH, INVENTORY_PANEL_HEIGHT,
        this.font
    );
    this.addRenderableWidget(inventoryPanelWidget);

    // 隐藏默认的背包标签
    this.inventoryLabelY = 10000;
}
```

### 设计优势

1. **视觉与逻辑分离**：Widget 只负责渲染背景，ScreenHandler 管理插槽逻辑
2. **居中显示**：通过计算 `(imageWidth - panelWidth) / 2` 确保居中
3. **背景限制**：背景渲染严格限制在 Widget 范围内，不会超出
4. **API 兼容性**：不依赖 `Inventory` 对象，避免 26.1.2 API 变更问题
5. **可复用性**：可轻松应用到其他需要自定义物品栏渲染的 GUI

## 验证步骤

```bash
# 编译检查
./gradlew build

# 运行客户端测试
./gradlew runClient
```

成功标志：
- GUI 显示深色背景
- 三个面板清晰分隔
- 按钮完全在背景区域内
- 背包面板居中显示在编辑器下方
- 插槽背景与实际插槽位置精确对齐
- 文本内容可见且位置正确
