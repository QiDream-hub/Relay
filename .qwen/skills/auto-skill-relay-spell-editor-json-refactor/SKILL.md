---
name: relay-spell-editor-json-refactor
description: 基于 BookViewScreen 重构法术编辑器为 JSON 文本编辑模式
source: auto-skill
extracted_at: '2026-06-24T00:00:00.000Z'
---

# 法术编辑器 JSON 文本重构模式

## 背景
将法术编辑器从复杂的 Widget 系统（程序列表 + 参数输入框）重构为可直接编辑的 JSON 文本窗口，借鉴 BookEditScreen 的多行文本编辑机制。

## 核心设计

### 1. JsonEditorWidget 组件
```java
public class JsonEditorWidget extends AbstractWidget {
    // 内部使用 MultiLineEditBox 进行多行文本编辑（参考 BookEditScreen）
    private final MultiLineEditBox editBox;

    public JsonEditorWidget(int x, int y, int width, int height, Font font) {
        this.editBox = MultiLineEditBox.builder()
            .setShowDecorations(false)
            .setTextColor(TEXT_COLOR)
            .setCursorColor(TEXT_COLOR)
            .setShowBackground(false)
            .setTextShadow(false)
            .build(font, width, height, Component.empty());
        
        this.editBox.setCharacterLimit(Integer.MAX_VALUE);
        this.editBox.setLineLimit(Integer.MAX_VALUE);  // 不限制行数
        this.editBox.setValueListener(this::onTextChanged);
    }

    // 在光标位置插入文本
    public void insertAtCursor(String text) {
        String current = this.editBox.getValue();
        // MultiLineEditBox 没有公开的光标位置 API，直接在末尾追加
        if (!current.isEmpty() && !current.endsWith("\n")) {
            text = "\n" + text;
        }
        this.editBox.setValue(current + text, true);
    }
}
```

**关键特性**：
- 使用 `MultiLineEditBox` 作为内部文本编辑器（参考 `BookEditScreen`）
- **原生支持 Enter 键换行**（`EditBox` 不支持）
- `setLineLimit(Integer.MAX_VALUE)` 不限制行数
- `setScrollAmount()` 控制滚动
- `getInnerHeight()` 获取内容高度
- 右侧滚动条导航
- JSON 解析错误提示（红色显示在底部）

### 2. SpellEditorScreen 布局
```java
// 左侧面板：操作列表 + 类型列表（各 120px 宽）
operationListWidget = new OperationListWidget(left, listTop, 120, 130, ...);
typeListWidget = new TypeListWidget(left, typeListTop, 120, 90, ...);

// 右侧面板：JSON 编辑器（占据剩余空间）
int editorWidth = GUI_WIDTH - PANEL_WIDTH - PANEL_PADDING * 3;
jsonEditorWidget = new JsonEditorWidget(editorX, editorY, editorWidth, editorHeight, font);

// 功能按钮：格式化、加载、保存
formatButton = Button.builder(Component.literal("格式化"), btn -> onFormat());
loadButton = Button.builder(Component.literal("加载"), btn -> onLoad());
saveButton = Button.builder(Component.literal("保存"), btn -> onSave());
```

### 3. 点击添加逻辑
```java
// 点击操作列表：在光标位置插入操作 JSON
private void onOperationClicked(String opId) {
    String jsonText = "{\"id\":\"" + opId + "\"}\n";
    jsonEditorWidget.insertAtCursor(jsonText);
}

// 点击类型列表：在光标位置插入数据类型 JSON
private void onTypeClicked(String typeId) {
    String jsonText = "{\"id\":\"" + typeId + "\"}\n";
    jsonEditorWidget.insertAtCursor(jsonText);
}
```

### 4. JSON 格式化
```java
// ProgramCompiler.toPrettyJson()
public static String toPrettyJson(JsonArray array) {
    StringBuilder sb = new StringBuilder();
    sb.append("[\n");
    for (int i = 0; i < array.size(); i++) {
        sb.append("  ").append(array.get(i).toString());
        if (i < array.size() - 1) sb.append(",");
        sb.append("\n");
    }
    sb.append("]");
    return sb.toString();
}
```

### 5. 保存流程
```java
private void onSave() {
    // 1. 从 JSON 编辑器获取内容
    String jsonStr = jsonEditorWidget.getJsonContent();

    // 2. 解析为 List<Executable>
    List<Executable> program = ProgramCompiler.compileFromJson(jsonStr);

    // 3. 转换为 NBT 并同步到服务端 BlockEntity
    ListTag programList = ProgramCompiler.toNbt(program);
    ClientPlayNetworking.send(new C2S_ProgramModifiedPayload(programTag));

    // 4. 保存到磁盘
    ClientPlayNetworking.send(new C2S_SaveSpellDiskPayload());
}
```

## Minecraft 组件选择

### EditBox vs MultiLineEditBox

| 特性 | EditBox | MultiLineEditBox |
|------|---------|------------------|
| 多行支持 | ❌ 单行 | ✅ 多行 |
| Enter 键换行 | ❌ 需要手动拦截 | ✅ 原生支持 |
| 滚动控制 | `setY()` 手动 | `setScrollAmount()` |
| 内容高度 | 手动计算 | `getInnerHeight()` |
| 适用场景 | 单行输入 | 多行文本（如 JSON 编辑器） |

### MultiLineEditBox 关键 API
```java
// 创建
MultiLineEditBox.builder()
    .setShowDecorations(false)
    .setTextColor(color)
    .setCursorColor(color)
    .setShowBackground(false)
    .build(font, width, height, narration);

// 设置
setCharacterLimit(Integer.MAX_VALUE);
setLineLimit(Integer.MAX_VALUE);
setValueListener(consumer);
setScrollAmount(amount);

// 获取
getValue();
getInnerHeight();
isFocused();
```

## 文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `JsonEditorWidget.java` | 新建 | JSON 文本编辑核心组件，使用 `MultiLineEditBox` |
| `SpellEditorScreen.java` | 重构 | 移除旧 Widget（ProgramListWidget, SignatureInputWidget），集成新编辑器 |
| `TypeListWidget.java` | 简化 | 移除选中状态，改为点击直接添加 |
| `ProgramCompiler.java` | 增强 | 添加 `toPrettyJson()` 方法 |

## 测试验证

1. **编译检查**: `./gradlew build` 无错误
2. **功能测试**:
   - 打开编辑器，查看 JSON 显示
   - 点击操作/类型列表，验证在光标位置插入
   - **按 Enter 键，验证正常换行**
   - 编辑多行 JSON，验证正确显示和滚动
   - 制造 JSON 错误，验证错误提示
   - 点击格式化，验证缩进美化
   - 保存/加载磁盘，验证数据持久化

## 注意事项

1. **MultiLineEditBox 没有公开的光标位置 API**
   - `insertAtCursor()` 只能在末尾追加
   - 如需精确插入，需要用户手动定位光标

2. **滚动控制**
   ```java
   // 使用 setScrollAmount() 而不是 setY()
   editBox.setScrollAmount(scrollOffset);
   ```

3. **内容高度获取**
   ```java
   int contentHeight = editBox.getInnerHeight();
   int maxScroll = contentHeight - visibleHeight;
   ```

4. **Widget 事件转发**: 在 `mouseClicked()`、`keyPressed()` 中优先转发给 `JsonEditorWidget`

5. **简化实现**: 移除了自定义语法高亮，让 `MultiLineEditBox` 自己渲染文本和光标
