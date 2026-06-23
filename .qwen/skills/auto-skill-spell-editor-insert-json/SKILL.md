---
name: spell-editor-insert-json
description: 法术编辑器中点击操作/类型时调用 toJson() 生成 JSON 并插入编辑器的实现模式
source: auto-skill
extracted_at: '2026-06-24T00:00:00.000Z'
---

# 法术编辑器插入 JSON 实现模式

## 问题背景

在法术编辑器中，用户点击左侧的操作列表或数据类型列表时，需要在右侧 JSON 编辑器中插入对应的 JSON 表示。

**需求**：
1. 插入的 JSON 必须调用 `Executable.toJson()` 方法生成，确保格式正确
2. 支持操作（如 `relay:add`）和数据类型（如 `relay:number`）
3. 在光标位置插入（受 Minecraft API 限制，当前在末尾追加）

## 实现方案

### 1. 使用 OperationRegistry 获取实例

```java
// SpellEditorScreen.java
private void onOperationClicked(String opId) {
    // 从注册表获取操作实例
    OperationRegistry.get(opId).ifPresent(op -> {
        JsonObject json = new JsonObject();
        ((qdream.relay.mc.base.Operation) op).toJson(json);
        jsonEditorWidget.insertAtCursor(json.toString() + "\n");
    });
}

private void onTypeClicked(String typeId) {
    // 从注册表获取数据类型实例（默认值）
    OperationRegistry.get(typeId).ifPresent(data -> {
        JsonObject json = new JsonObject();
        ((qdream.relay.mc.base.Operation) data).toJson(json);
        jsonEditorWidget.insertAtCursor(json.toString() + "\n");
    });
}
```

### 2. 导入依赖

```java
import com.google.gson.JsonObject;
import qdream.relay.mc.OperationRegistry;
```

### 3. 生成的 JSON 格式

**操作**（无状态单例）：
```json
{"id":"relay:add"}
```

**数据类型**（有状态，使用默认值）：
```json
{"id":"relay:number","value":{"number":0.0}}
{"id":"relay:boolean","value":{"boolean":false}}
{"id":"relay:string","value":{"string":""}}
```

## 架构说明

### OperationRegistry 统一注册表

```java
// OperationRegistry.java
public static Optional<Executable> get(String id) {
    Entry entry = REGISTRY.get(id);
    return entry != null ? Optional.of(entry.create()) : Optional.empty();
}

public static Set<String> getAllOperationIds() { ... }
public static Set<String> getAllDataIds() { ... }
```

**设计要点**：
- 操作和数据类型共用同一注册表
- 操作是单例，`create()` 返回自身
- 数据类型是工厂，`create()` 返回默认值实例（如 `NumberIota(0.0)`）

### Operation 基类的 toJson()

```java
// Operation.java
public void toJson(JsonObject json) {
    json.addProperty("id", id);
}

// NumberIota.java (子类 override)
@Override
public void toJson(JsonObject json) {
    super.toJson(json);  // 写入 id
    JsonObject value = new JsonObject();
    value.addProperty("number", this.value);
    json.add("value", value);
}
```

## Minecraft API 限制

### MultiLineEditBox 光标控制

Minecraft 26.1.2 的 `MultiLineEditBox` 没有公开光标位置 API：
- `cursor()` 方法在私有的 `textField` 字段中
- `setCursor(int)` 方法不可访问
- 无法精确控制插入位置

**当前实现**（在末尾追加）：
```java
// JsonEditorWidget.java
public void insertAtCursor(String text) {
    String current = this.editBox.getValue();
    
    // 简单地在末尾追加，前导换行
    if (!current.isEmpty() && !current.endsWith("\n")) {
        text = "\n" + text;
    }
    this.editBox.setValue(current + text, true);
}
```

**用户体验**：
- 用户点击编辑器定位光标
- 点击操作/类型列表
- 内容在末尾追加（而非光标位置）

### 可能的解决方案

1. **等待 Minecraft 开放 API** - 未来版本可能公开光标控制
2. **使用反射** - 不推荐，可能导致兼容性问题
3. **自定义文本编辑器** - 工作量大，但完全控制

## 扩展自定义类型

其他 mod 注册的数据类型会自动支持此功能：

```java
// 其他 mod 注册自定义类型
OperationRegistry.register("mymod:custom_data",
    new DataEntry(() -> new CustomDataIota())
);

// 用户点击时自动生成正确的 JSON
// {"id":"mymod:custom_data","value":{...}}
```

## 相关文件

- `src/client/java/qdream/relay/client/screen/SpellEditorScreen.java` - 编辑器主屏幕
- `src/client/java/qdream/relay/client/screen/widget/editor/JsonEditorWidget.java` - JSON 编辑器 Widget
- `src/main/java/qdream/relay/mc/OperationRegistry.java` - 统一注册表
- `src/main/java/qdream/relay/mc/base/Operation.java` - 操作基类（提供 `toJson()`）
- `src/main/java/qdream/relay/types/NumberIota.java` - 数据类型示例（override `toJson()`）

## 注意事项

1. **类型转换** - 需要 cast 为 `Operation` 才能调用 `toJson()`
2. **Optional 处理** - `OperationRegistry.get()` 返回 `Optional`，使用 `ifPresent()` 安全访问
3. **默认值** - 数据类型使用默认值（如 `NumberIota(0.0)`），用户可在编辑器中修改
4. **换行** - 插入的 JSON 后添加换行符，便于阅读
5. **错误处理** - 如果注册表中不存在该 ID，`ifPresent()` 会自动跳过，不会崩溃
