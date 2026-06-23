---
name: custom-multiline-editbox
description: 自定义 MultiLineEditBox 实现精确光标控制的模式 - 继承 AbstractTextAreaWidget 实现完整的多行文本编辑功能
source: auto-skill
extracted_at: '2026-06-24T12:30:00.000Z'
---

# 自定义 MultiLineEditBox 实现精确光标控制

## 问题背景

Minecraft 26.1.2 的 `MultiLineEditBox` 类将底层的 `MultilineTextField` 字段设为 `private`，且没有公开光标位置控制方法（如 `cursor()`, `setCursor()`）。这导致无法在精确的光标位置插入文本，只能在末尾追加。

## 解决方案

继承 `AbstractTextAreaWidget` 实现自定义的多行文本编辑器，完全控制光标位置和选区。

## 核心实现

### 1. 继承 AbstractTextAreaWidget

```java
public class CustomMultiLineEditBox extends AbstractTextAreaWidget {
    private StringBuilder text = new StringBuilder();
    private int cursorPos = 0;           // 光标位置（字符索引）
    private int selectionStart = -1;     // 选区起始位置（-1 表示无选区）
    private double scrollOffset = 0;     // 滚动偏移量
    
    public CustomMultiLineEditBox(
        Font font, int x, int y, int width, int height,
        Component placeholder, int textColor, boolean textShadow,
        int cursorColor, boolean showBackground, boolean showDecorations
    ) {
        super(x, y, width, height, Component.empty(), defaultSettings(4), showBackground, showDecorations);
        // 初始化字段...
    }
}
```

### 2. 实现光标控制方法

```java
// 获取/设置光标位置
public int getCursorPos() { return this.cursorPos; }
public void setCursorPos(int pos) {
    this.cursorPos = Math.max(0, Math.min(pos, this.text.length()));
    this.selectionStart = -1;
    this.ensureCursorVisible();
}

// 在光标位置插入文本
public void insertText(String textToInsert) {
    if (hasSelection()) {
        // 有选区时替换选区
        int start = Math.min(this.cursorPos, this.selectionStart);
        int end = Math.max(this.cursorPos, this.selectionStart);
        this.text.replace(start, end, textToInsert);
        this.cursorPos = start + textToInsert.length();
        this.selectionStart = -1;
    } else {
        // 无选区时插入
        this.text.insert(this.cursorPos, textToInsert);
        this.cursorPos += textToInsert.length();
    }
    this.ensureCursorVisible();
    this.onValueChanged();
}
```

### 3. 实现键盘事件处理

```java
@Override
public boolean keyPressed(KeyEvent event) {
    if (!this.isFocused()) return false;
    
    // 方向键移动光标
    if (event.isDown()) { moveCursorDown(); return true; }
    if (event.isUp()) { moveCursorUp(); return true; }
    if (event.isLeft()) { moveCursorLeft(event.hasShiftDown()); return true; }
    if (event.isRight()) { moveCursorRight(event.hasShiftDown()); return true; }
    
    // Home/End 键
    if (event.key() == 268) { moveToLineStart(event.hasShiftDown()); return true; }
    if (event.key() == 269) { moveToLineEnd(event.hasShiftDown()); return true; }
    
    // 删除键
    if (event.key() == 259) { deleteBackward(); return true; }
    if (event.key() == 261) { deleteForward(); return true; }
    
    // Enter 键
    if (event.key() == 257) { insertText("\n"); return true; }
    
    // Ctrl+A/C/V/X 快捷键
    if (event.hasControlDown() && event.key() == 65) { /* 全选 */ }
    if (event.hasControlDown() && event.key() == 67) { /* 复制 */ }
    if (event.hasControlDown() && event.key() == 86) { /* 粘贴 */ }
    if (event.hasControlDown() && event.key() == 88) { /* 剪切 */ }
    
    return false;
}
```

### 4. 实现鼠标事件处理

```java
@Override
public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    if (this.visible && this.isMouseOver(event.x(), event.y())) {
        this.setFocused(true);
        this.focusedTime = Util.getMillis();
        
        if (doubleClick) {
            selectWordAtCursor();  // 双击选中单词
        } else {
            int newPos = calculateCursorPos(event.x(), event.y());
            if (event.hasShiftDown() && this.selectionStart >= 0) {
                this.cursorPos = newPos;  // Shift+ 点击扩展选区
            } else {
                this.cursorPos = newPos;
                this.selectionStart = newPos;  // 新选区
            }
        }
        return true;
    }
    return false;
}
```

### 5. 实现渲染方法

```java
@Override
protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    String value = this.text.toString();
    boolean showCursor = this.isFocused() && isCursorVisible();
    
    // 按行渲染
    int lineStart = 0, lineEnd = 0;
    while (lineStart <= value.length()) {
        lineEnd = value.indexOf('\n', lineStart);
        if (lineEnd == -1) lineEnd = value.length();
        
        // 渲染选区高亮
        if (hasSelection()) { /* 渲染高亮 */ }
        
        // 渲染光标
        if (showCursor && cursorPos >= lineStart && cursorPos <= lineEnd) {
            String textBeforeCursor = value.substring(lineStart, cursorPos);
            int cursorX = innerLeft + font.width(textBeforeCursor);
            graphics.fill(cursorX, drawTop, cursorX + 1, drawTop + LINE_HEIGHT, cursorColor);
        }
        
        // 渲染文本
        String lineText = value.substring(lineStart, lineEnd);
        graphics.text(font, lineText, innerLeft, drawTop, textColor, textShadow);
        
        drawTop += LINE_HEIGHT;
        lineStart = lineEnd + 1;
    }
}
```

### 6. 字符输入处理（重要）

**问题**：输入字母时显示为数字（如输入 "a" 显示 "97"）

**原因**：`String.valueOf(int)` 将 Unicode 代码点转换为数字字符串

**解决方案**：使用 `Character.toString(int)` 正确转换

```java
@Override
public boolean charTyped(CharacterEvent event) {
    if (this.isFocused() && event.isAllowedChatCharacter()) {
        // ✅ 正确：使用 Character.toString() 将 codepoint 转换为字符串
        insertText(Character.toString(event.codepoint()));
        return true;
    }
    return false;
}
```

### 7. 辅助方法

```java
// 计算光标位置（屏幕坐标→字符索引）
private int calculateCursorPos(double mouseX, double mouseY) {
    double relY = mouseY - getInnerTop() + scrollOffset;
    int targetLine = (int) (relY / LINE_HEIGHT);
    
    // 找到目标行的起始位置
    int lineStart = getLineStart(targetLine);
    int lineEnd = getLineEnd(targetLine);
    
    // 计算在该行中的字符位置
    String lineText = text.substring(lineStart, lineEnd);
    int charIndex = 0, accumulatedWidth = 0;
    for (int i = 0; i < lineText.length(); i++) {
        int charWidth = font.width(String.valueOf(lineText.charAt(i)));
        if (accumulatedWidth + charWidth / 2 > relX) break;
        accumulatedWidth += charWidth;
        charIndex++;
    }
    return lineStart + charIndex;
}

// 获取行起始/结束位置
private int getLineStart(int line) { /* ... */ }
private int getLineEnd(int line) { /* ... */ }
private int getCurrentLine() { /* ... */ }
```

## 使用示例

```java
// 创建自定义编辑器
CustomMultiLineEditBox editBox = new CustomMultiLineEditBox(
    font, x, y, width, height,
    Component.empty(), textColor, false, cursorColor, false, false
);

// 设置文本变化监听器
editBox.setValueListener(() -> { /* 处理变化 */ });

// 在光标位置插入文本
editBox.insertText("{\"id\":\"relay:add\"}\n");

// 获取/设置光标位置
int cursorPos = editBox.getCursorPos();
editBox.setCursorPos(10);
```

## 关键要点

1. **避免依赖内部类**：不使用 `MultilineTextField`、`IMEPreeditOverlay`、`TextCursorUtils` 等 Minecraft 内部类
2. **手动管理状态**：使用 `StringBuilder` 存储文本，`int` 存储光标位置和选区
3. **实现完整的滚动逻辑**：重写 `setScrollAmount()`, `scrollAmount()`, `scrollRate()` 方法
4. **光标闪烁效果**：使用 `Util.getMillis()` 计算时间，每 300ms 切换一次可见性
5. **选区处理**：在插入/删除文本时检查选区，有选区时先替换选区内容
6. **字符编码处理**：使用 `Character.toString(int)` 而非 `String.valueOf(int)` 转换代码点

## 适用场景

- 需要在精确光标位置插入/删除文本
- 需要自定义文本渲染样式（如语法高亮、错误提示）
- 需要完全控制光标和选区行为
- Minecraft 原生 `MultiLineEditBox` 或 `TextField` 不满足需求时
