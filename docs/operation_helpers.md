# OperationHelpers 工具类使用指南

## 概述

`OperationHelpers` 位于 `qdream.relay.operations.base` 包，提取了操作中重复使用的公共逻辑，减少代码重复。

## 功能分类

### 1. 世界交互器检查

#### 检查世界交互器存在
```java
// 旧代码
ShellContainer container = getShellContainer(executor);
if (container == null || !container.hasWorldInteractor()) {
    executor.triggerMishap("操作需要世界交互器");
    return;
}

// 新代码
if (!OperationHelpers.checkWorldInteractor(executor, "操作名")) {
    return;
}
```

#### 获取世界交互器物品
```java
// 旧代码
ShellContainer container = getShellContainer(executor);
ItemStack interactor = container.getInteractorStack();

// 新代码
Optional<ItemStack> interactorOpt = OperationHelpers.getWorldInteractorStack(executor);
```

#### 范围检查
```java
// 旧代码
if (!WorldInteractorItem.isInRange(interactor, sourcePos, targetPos)) {
    executor.triggerMishap("操作超出世界交互器范围");
    return;
}

// 新代码
if (!OperationHelpers.checkInRange(executor, "操作名", sourcePos, targetPos)) {
    return;
}
```

### 2. 上下文获取

#### 获取 Level
```java
// 旧代码
Optional<Level> levelOpt = executor.getContext("level", Level.class);
if (levelOpt.isEmpty()) {
    executor.triggerMishap("无法获取世界");
    return;
}

// 新代码
Optional<Level> levelOpt = OperationHelpers.getLevel(executor, "操作名");
if (levelOpt.isEmpty()) {
    return; // 错误已自动触发
}
```

#### 获取自身位置
```java
// 旧代码 - 需要 10+ 行
Vec3 sourcePos = new Vec3(0, 0, 0);
var selfOpt = executor.getContext("self", Object.class);
if (selfOpt.isPresent()) {
    Object self = selfOpt.get();
    if (self instanceof Entity entity) {
        sourcePos = entity.position();
    } else if (self instanceof BlockEntity blockEntity) {
        sourcePos = Vec3.atCenterOf(blockEntity.getBlockPos());
    }
}

// 新代码 - 1 行
Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
```

### 3. 类型安全的栈弹出

#### 弹出单个类型
```java
// 旧代码
Executable exe = executor.popData();
if (exe == null) {
    executor.triggerMishap("数据栈不足");
    return;
}
if (!(exe instanceof VectorData vec)) {
    executor.triggerMishap("期望 vector 类型");
    return;
}

// 新代码
VectorData vec = OperationHelpers.popVector(executor, "操作名");
if (vec == null) {
    return; // 错误已自动触发
}
```

#### 支持的类型
- `popNumber()` - NumberData
- `popBoolean()` - BooleanData
- `popVector()` - VectorData
- `popEntity()` - EntityData
- `popBlockEntity()` - BlockEntityData
- `popList()` - ListData
- `popString()` - StringData
- `popType()` - TypeData

#### 通用弹出
```java
// 弹出自定义类型
MyCustomData data = OperationHelpers.popAsType(executor, MyCustomData.class, "操作名");
```

### 4. 便捷转换

```java
// 旧代码
double value = ((NumberData) exe).asDouble();
boolean flag = ((BooleanData) exe).asBoolean();
Vec3 vec = ((VectorData) exe).asVector();

// 新代码
double value = OperationHelpers.asDouble(exe);
boolean flag = OperationHelpers.asBoolean(exe);
Vec3 vec = OperationHelpers.asVector(exe);
```

### 5. 栈操作

#### 弹出多个参数
```java
// 旧代码 - 需要循环
Executable[] result = new Executable[3];
for (int i = 2; i >= 0; i--) {
    result[i] = executor.popData();
    if (result[i] == null) {
        executor.triggerMishap("数据栈不足");
        return;
    }
}

// 新代码
Executable[] args = OperationHelpers.popMultiple(executor, 3);
if (args == null) {
    return;
}
```

#### 检查栈大小
```java
// 旧代码
if (executor.getDataStackSize() < 2) {
    executor.triggerMishap("需要 2 个参数");
    return;
}

// 新代码
if (!OperationHelpers.checkStackSize(executor, 2, "操作名")) {
    return;
}
```

## 完整示例

### 示例 1：简单的向量操作
```java
// 旧代码
@Override
public void execute(StateMachine executor) {
    Executable bExe = executor.popData();
    Executable aExe = executor.popData();
    
    if (bExe == null || aExe == null) {
        executor.triggerMishap("数据栈不足");
        return;
    }
    
    if (!(bExe instanceof VectorData bVec) || !(aExe instanceof VectorData aVec)) {
        executor.triggerMishap("期望 vector 类型");
        return;
    }
    
    Vec3 result = aVec.asVector().add(bVec.asVector());
    executor.pushData(new VectorData(result));
}

// 新代码
@Override
public void execute(StateMachine executor) {
    VectorData bVec = OperationHelpers.popVector(executor, "vector_add");
    if (bVec == null) return;
    
    VectorData aVec = OperationHelpers.popVector(executor, "vector_add");
    if (aVec == null) return;
    
    Vec3 result = aVec.asVector().add(bVec.asVector());
    executor.pushData(new VectorData(result));
}
```

### 示例 2：需要世界交互器的操作
```java
// 旧代码 - 约 50 行
@Override
public void execute(StateMachine executor) {
    ShellContainer container = getShellContainer(executor);
    if (container == null || !container.hasWorldInteractor()) {
        executor.triggerMishap("操作需要世界交互器");
        return;
    }
    
    ItemStack interactor = container.getInteractorStack();
    
    // 获取自身位置 - 10+ 行
    Vec3 sourcePos = new Vec3(0, 0, 0);
    var selfOpt = executor.getContext("self", Object.class);
    if (selfOpt.isPresent()) {
        // ... 类型判断
    }
    
    // 弹出参数
    Executable posExe = executor.popData();
    if (posExe == null) {
        executor.triggerMishap("数据栈不足");
        return;
    }
    if (!(posExe instanceof VectorData pos)) {
        executor.triggerMishap("期望 vector 类型");
        return;
    }
    
    // 范围检查
    if (!WorldInteractorItem.isInRange(interactor, sourcePos, pos.asVector())) {
        executor.triggerMishap("超出范围");
        return;
    }
    
    // 获取世界
    Optional<Level> levelOpt = executor.getContext("level", Level.class);
    if (levelOpt.isEmpty()) {
        executor.triggerMishap("无法获取世界");
        return;
    }
    
    // ... 实际逻辑
}

// 新代码 - 约 20 行
@Override
public void execute(StateMachine executor) {
    if (!OperationHelpers.checkWorldInteractor(executor, "操作名")) {
        return;
    }
    
    VectorData pos = OperationHelpers.popVector(executor, "操作名");
    if (pos == null) return;
    
    Vec3 sourcePos = OperationHelpers.getSelfPosition(executor);
    if (!OperationHelpers.checkInRange(executor, "操作名", sourcePos, pos.asVector())) {
        return;
    }
    
    Optional<Level> levelOpt = OperationHelpers.getLevel(executor, "操作名");
    if (levelOpt.isEmpty()) return;
    
    // ... 实际逻辑
}
```

## 最佳实践

1. **优先使用类型专用方法**：`popVector()` 比 `popAsType(..., VectorData.class, ...)` 更简洁
2. **错误消息一致性**：所有错误消息都包含操作名，便于调试
3. **早期返回**：工具方法失败时返回 null，调用者立即返回避免级联错误
4. **组合使用**：多个检查可以链式调用，每个检查后紧跟 `if (result == null) return;`

## 注意事项

- 工具类方法失败时会自动触发 `triggerMishap()`，无需重复处理
- 所有返回 null 的方法表示执行失败，应立即返回
- Optional 返回值表示需要调用者自行处理空值情况
