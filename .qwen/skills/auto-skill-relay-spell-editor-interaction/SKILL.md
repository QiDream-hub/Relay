---
name: relay-spell-editor-interaction
description: Relay 法术编辑器交互优化 - 编辑框聚焦取消、列表 Widget 设计模式、选中状态管理
source: auto-skill
extracted_at: '2026-06-24T12:00:00.000Z'
---

# Relay 法术编辑器交互优化

法术编辑器 GUI 中的交互问题解决方案，包括编辑框聚焦取消、列表 Widget 设计模式、选中状态管理等。

## 问题 1: 编辑框点击聚焦后无法取消聚焦

### 问题描述
点击 JSON 编辑框后获得聚焦，但点击外部空白区域时无法取消聚焦，导致编辑框始终保持激活状态。

### 根本原因
- `JsonEditorWidget.mouseClicked()` 只处理点击在编辑器内部或滚动条的情况
- 当点击编辑器外部时，事件没有被任何 Widget 消费，但也没有触发取消聚焦逻辑
- `SpellEditorScreen.mouseClicked()` 虽然会转发事件，但编辑器已经消费了点击事件

### 解决方案：在 Screen 层面处理外部点击

```java
@Override
public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    // 优先转发给 JSON 编辑器
    if (jsonEditorWidget != null && jsonEditorWidget.visible) {
        if (jsonEditorWidget.mouseClicked(event, doubleClick)) {
            return true;
        }
    }

    // 转发给其他自定义 Widget
    for (var widget : this.children()) {
        if (widget instanceof net.minecraft.client.gui.components.AbstractWidget aw
                && aw.visible
                && event.x() >= aw.getX() && event.x() < aw.getX() + aw.getWidth()
                && event.y() >= aw.getY() && event.y() < aw.getY() + aw.getHeight()) {
            if (aw.mouseClicked(event, doubleClick)) {
                return true;
            }
        }
    }

    // 点击空白区域取消编辑器聚焦
    if (jsonEditorWidget != null && jsonEditorWidget.isFocused()) {
        jsonEditorWidget.setFocused(false);
    }

    return super.mouseClicked(event, doubleClick);
}
```

### 关键设计点
1. **事件处理顺序**：先转发给 Widget，最后处理外部点击
2. **聚焦检查**：只在编辑器已聚焦时才取消
3. **位置**：在所有 Widget 事件处理后，`super.mouseClicked()` 调用前

## 问题 2: 列表 Widget 布局优化

### 优化目标
- 统一操作列表和类型列表的视觉风格
- 改进视觉层次（标题栏、分隔线、边框）
- 添加选中状态跟踪
- 优化滚动条样式和交互
- 提高空间利用率

### 统一的设计风格

```java
// 布局常量
private static final int LINE_HEIGHT = 11;      // 行高（优化后）
private static final int PADDING = 3;           // 内边距（优化后）
private static final int HEADER_HEIGHT = 16;    // 标题栏高度

// 颜色方案
private static final int BG_COLOR = 0xFF1E1E1E;         // 深灰背景
private static final int HEADER_BG = 0xFF252525;        // 标题栏背景（稍亮）
private static final int BORDER_COLOR = 0xFF3A3A3A;     // 边框颜色
private static final int TEXT_COLOR = 0xFF00AA00;       // 文本颜色（操作列表绿色）
private static final int HOVER_COLOR = 0xFF55FF55;      // 悬停文本颜色
private static final int HOVER_BG = 0x3000FF00;         // 悬停背景（半透明）
private static final int SELECTED_BG = 0x4000AA00;      // 选中背景（半透明）
private static final int SCROLLBAR_COLOR = 0xFF505050;  // 滚动条颜色
private static final int SCROLLBAR_HOVER = 0xFF707070;  // 滚动条悬停颜色
private static final int SCROLLBAR_BG = 0xFF2A2A2A;     // 滚动条背景
```

### 标题栏设计

```java
@Override
protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, ...) {
    int x = getX();
    int y = getY();

    // 背景
    graphics.fill(x, y, x + this.width, y + this.height, BG_COLOR);
    
    // 外边框
    graphics.outline(x, y, this.width, this.height, BORDER_COLOR);

    // 标题栏背景
    graphics.fill(x, y, x + this.width, y + HEADER_HEIGHT, HEADER_BG);
    graphics.horizontalLine(x, x + this.width, y + HEADER_HEIGHT, BORDER_COLOR);

    // 标题文字
    graphics.text(this.font, "可用操作", x + PADDING + 2, y + 4, 0xFF00FF00);

    // 列表内容渲染...
}
```

### 选中状态管理

```java
/** 当前选中的条目索引，-1 表示无选中 */
private int selectedIndex = -1;

@Override
public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    if (!this.visible) return false;
    
    if (event.button() == 0) {
        int index = getEntryAt(event.x(), event.y());
        if (index >= 0 && index < operations.size()) {
            selectedIndex = index;  // 先更新选中状态
            if (onOperationClicked != null) {
                onOperationClicked.accept(operations.get(index));
            }
            return true;
        }
    }
    return false;
}
```

### 渲染时处理选中和悬停

```java
// 渲染列表条目
for (int i = 0; i < visibleLines && (i + scrollOffset) < operations.size(); i++) {
    int dataIndex = i + scrollOffset;
    String op = operations.get(dataIndex);
    int entryY = textY + i * LINE_HEIGHT;

    boolean isHovered = (dataIndex == hoveredIndex);
    boolean isSelected = (dataIndex == selectedIndex);

    // 选中背景优先于悬停背景
    if (isSelected) {
        graphics.fill(x + PADDING, entryY - 1, x + this.width - PADDING - 4, 
                     entryY + LINE_HEIGHT - 1, SELECTED_BG);
    } else if (isHovered) {
        graphics.fill(x + PADDING, entryY - 1, x + this.width - PADDING - 4, 
                     entryY + LINE_HEIGHT - 1, HOVER_BG);
    }

    int color = isHovered ? HOVER_COLOR : TEXT_COLOR;
    graphics.text(this.font, op, textX, entryY, color);
}
```

### 优化的滚动条

```java
/** 滚动条悬停状态 */
private boolean scrollbarHovered = false;

/** 检查鼠标是否在滚动条上 */
private boolean isMouseOnScrollbar(double mouseX, double mouseY) {
    int sbX = getX() + this.width - 4;
    return mouseX >= sbX && mouseX <= getX() + this.width;
}

@Override
protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, ...) {
    // 更新悬停索引
    hoveredIndex = getEntryAt(mouseX, mouseY);
    scrollbarHovered = isMouseOnScrollbar(mouseX, mouseY);
    
    // 渲染滚动条...
}

private void renderScrollBar(GuiGraphicsExtractor graphics, int x, int y, int visibleLines) {
    int sbX = x + this.width - 4;
    int sbTop = y + HEADER_HEIGHT + PADDING;
    int sbHeight = this.height - HEADER_HEIGHT - PADDING * 2;

    // 滚动条背景
    graphics.fill(sbX, sbTop, sbX + 4, sbTop + sbHeight, SCROLLBAR_BG);

    // 滚动条滑块
    float ratio = (float) visibleLines / operations.size();
    int thumbHeight = Math.max(10, (int) (sbHeight * ratio));  // 最小高度 10px
    float scrollRatio = getMaxScroll() > 0 ? (float) scrollOffset / getMaxScroll() : 0;
    int thumbY = sbTop + (int) ((sbHeight - thumbHeight) * scrollRatio);

    // 悬停时变亮
    int thumbColor = scrollbarHovered ? SCROLLBAR_HOVER : SCROLLBAR_COLOR;
    graphics.fill(sbX, thumbY, sbX + 4, thumbY + thumbHeight, thumbColor);
}
```

### 空间优化计算

```java
/** 获取可视区域内可显示的最大行数 */
private int getVisibleLineCount() {
    return Math.max(1, (this.height - HEADER_HEIGHT - PADDING * 2) / LINE_HEIGHT);
}

/** 根据鼠标坐标计算条目索引 */
private int getEntryAt(double mouseX, double mouseY) {
    double relX = mouseX - (getX() + PADDING);
    double relY = mouseY - (getY() + HEADER_HEIGHT + PADDING);  // 减去标题栏
    if (relX < 0 || relX > this.width - PADDING * 2 - 4 || relY < 0) return -1;
    int index = scrollOffset + (int) (relY / LINE_HEIGHT);
    if (index < 0 || index >= operations.size()) return -1;
    return index;
}
```

### 底部计数提示

```java
// 底部计数提示
int countY = y + this.height - LINE_HEIGHT - 2;
graphics.text(this.font, operations.size() + " 个操作", x + PADDING, countY, 0xFF666666);
```

## 完整 Widget 结构

```
┌─────────────────────────┐
│ 可用操作      [标题栏]   │ ← HEADER_HEIGHT = 16px
├─────────────────────────┤ ← 分隔线
│ push                    │
│ pop                     │
│ add                     │ ← LINE_HEIGHT = 11px × N 行
│ ...                     │
│                         │
│                         │
│                         │
├─────────────────────────┤
│ 24 个操作     [计数提示] │ ← 底部提示
└─────────────────────────┘
```

## 关键改进总结

| 改进项 | 旧方案 | 新方案 |
|--------|--------|--------|
| 行高 | 12-14px | 11px |
| 内边距 | 4px | 3px |
| 标题 | 无独立标题栏 | 独立标题栏 + 分隔线 |
| 选中状态 | 无 | 有（`selectedIndex`） |
| 滚动条宽度 | 3px | 4px |
| 滚动条悬停 | 无 | 有（颜色变亮） |
| 外边框 | 无 | 有 |
| 背景色 | `#1A1A1A` | `#1E1E1E` |

## 应用模式

### 创建新的列表 Widget
1. 继承 `AbstractWidget`
2. 定义统一的布局常量和颜色
3. 实现 `selectedIndex` 跟踪选中状态
4. 实现 `scrollbarHovered` 跟踪滚动条悬停
5. 在 `mouseClicked` 中更新选中状态
6. 在 `extractWidgetRenderState` 中渲染标题栏、列表项、滚动条

### 集成到 Screen
1. 在 `init()` 中创建 Widget 并设置回调
2. 在 `mouseClicked()` 中处理外部点击取消聚焦
3. 确保事件正确转发给子 Widget

## 验证步骤

```bash
# 编译检查
./gradlew build

# 运行客户端测试
./gradlew runClient
```

成功标志：
- 点击编辑框外部时取消聚焦
- 两个列表 Widget 视觉风格一致
- 点击列表项时显示选中背景
- 滚动条悬停时颜色变亮
- 标题栏清晰区分
