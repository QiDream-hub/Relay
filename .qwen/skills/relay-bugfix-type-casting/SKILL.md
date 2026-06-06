---
name: relay-bugfix-type-casting
description: Relay 模组类型转换 bug 修复经验——统一使用 Executable 类型进行数据栈操作
source: auto-skill
extracted_at: '2026-06-07T00:00:00.000Z'
---

# Relay 类型转换 Bug 修复经验

2026-06-07 精简模型后修复编译错误的经验总结。

## 问题背景

精简后的架构将所有 Iota 类型统一为 `Executable` 接口，但原有代码中存在大量类型转换错误：

1. `executor.popData()` 返回 `Executable`，但代码期望 `Spell`
2. 所有操作类使用 `Spell channelData = (Spell) executor.popData()` 模式
3. `CommunicationSystem` 返回 `Executable`，但代码期望 `Spell`

## 核心问题

### 1. Executable 接口缺少 getId() 方法

**问题**：类型检查时需要调用 `getId()`，但接口未定义此方法。

**修复**：
```java
// engine/Executable.java
public interface Executable {
    String getId();  // 添加此方法
    void execute(StateMachine executor);
}
```

### 2. Operation 抽象类未覆盖 getId()

**问题**：`Operation` 类有 `id` 字段但未标记 `@Override`。

**修复**：
```java
// mc/base/Operation.java
public abstract class Operation implements Executable {
    protected final String id;
    
    @Override  // 添加 @Override 注解
    public String getId() {
        return id;
    }
}
```

### 3. 所有操作的类型转换错误

**问题模式**：
```java
// ❌ 错误：Spell 无法转换为具体类型
Spell channelData = (Spell) executor.popData();
if (!(channelData instanceof NumberIota channel)) { ... }
```

**修复模式**：
```java
// ✅ 正确：使用 Executable 类型
Executable channelData = executor.popData();
if (!(channelData instanceof NumberIota channel)) { ... }
```

**影响文件**：
- `operations/arithmetic/*Op.java` - 算术操作
- `operations/logic/*Op.java` - 逻辑操作
- `operations/communication/*Op.java` - 通信操作
- `operations/list/*Op.java` - 列表操作
- `operations/control/*Op.java` - 控制流操作
- `operations/base/*Op.java` - 基础操作

### 4. 缺失的辅助方法

**NumberIota 缺少方法**：
```java
// types/NumberIota.java
public int asInt() { return (int) value; }
public double getValue() { return value; }
public boolean isInteger() { return value == (int) value; }
```

**VectorIota 缺少方法**：
```java
// types/VectorIota.java
public Vec3 getVec3() { return value; }
```

**StateMachine 缺少方法**：
```java
// engine/StateMachine.java
private boolean hasWorldInteractor = false;

public boolean hasWorldInteractor() { return hasWorldInteractor; }
public void setHasWorldInteractor(boolean has) { this.hasWorldInteractor = has; }
```

### 5. 导入路径错误

**问题**：`OperationRegistry` 从 `engine` 移至 `mc` 包，导入未更新。

**修复**：
```java
// ❌ 错误
import qdream.relay.engine.OperationRegistry;

// ✅ 正确
import qdream.relay.mc.OperationRegistry;
```

**影响文件**：
- `screen/SpellEditorScreenHandler.java`
- `networking/RelayServerNetworking.java`
- `client/editor/SpellEditorScreen.java`
- `client/networking/RelayClientNetworking.java`

### 6. ProgramBlock 类缺失

**问题**：`ProgramBlock` 类被删除但多处仍在引用。

**修复**：创建新类 `types/ProgramBlock.java`：
```java
public class ProgramBlock extends Data {
    private final List<Executable> items;

    public ProgramBlock(List<Executable> items) {
        super("relay:list", 0);
        this.items = new ArrayList<>(items);
    }

    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);  // 数据执行时压栈
    }

    public List<Executable> getItems() { return new ArrayList<>(items); }

    public void expandToProgramStack(StateMachine executor) {
        List<Executable> reversed = new ArrayList<>(items);
        Collections.reverse(reversed);
        for (Executable item : reversed) {
            executor.pushProgram(item);
        }
    }
}
```

### 7. NbtSerializer 匿名类问题

**问题**：反序列化操作时使用 `new Operation(id)`，但 `Operation` 是抽象类。

**修复**：
```java
// mc/NbtSerializer.java
default -> new Operation(id, 0) {
    @Override
    public void execute(StateMachine executor) {
        executor.triggerMishap("未知操作：" + id);
    }
};
```

## 修复检查清单

### 接口层
- [ ] `Executable` 添加 `getId()` 方法
- [ ] `Operation` 添加 `@Override` 到 `getId()`

### 类型层
- [ ] `NumberIota` 添加 `asInt()`, `getValue()`, `isInteger()`
- [ ] `VectorIota` 添加 `getVec3()`
- [ ] 创建 `ProgramBlock` 类

### 执行器层
- [ ] `StateMachine` 添加 `hasWorldInteractor()` 相关方法

### 操作层（全部操作文件）
- [ ] `Spell channelData = (Spell) executor.popData()` → `Executable channelData = executor.popData()`
- [ ] 移除不必要的 `Spell` 导入
- [ ] 添加 `Executable` 导入

### 导入路径
- [ ] `engine.OperationRegistry` → `mc.OperationRegistry`
- [ ] `types.Operation` → `mc.base.Operation`（客户端代码）

### 序列化器
- [ ] `NbtSerializer` 修复匿名操作类
- [ ] `NbtSerializer` 添加 `StateMachine` 导入

### 客户端代码
- [ ] `SpellEditorScreen` 修复导入和类型转换
- [ ] `RelayClientNetworking` 修复导入

## 设计原则

### 1. 统一类型原则

所有数据栈操作使用 `Executable` 类型：
```java
Executable data = executor.popData();
if (data instanceof NumberIota num) { ... }
```

### 2. 类型安全原则

所有操作必须进行类型检查并报告错误：
```java
if (!(data instanceof NumberIota num)) {
    executor.triggerMishap("期望 number 类型，实际为：" + data.getId());
    return;
}
```

### 3. 继承复用原则

`Data extends Operation` 复用 `id` 和 `cost` 字段：
```java
public abstract class Data extends Operation {
    public Data(String id, int cost) {
        super(id, cost);  // cost=0 表示数据无消耗
    }
    
    @Override
    public void execute(StateMachine executor) {
        executor.pushData(this);  // 数据执行时压栈
    }
}
```

## 编译验证

修复后运行构建：
```bash
./gradlew build
```

预期输出：
```
BUILD SUCCESSFUL
```

## 关键教训

1. **接口变更影响广泛**：添加 `getId()` 到 `Executable` 影响所有实现类
2. **类型转换必须一致**：所有操作必须使用相同的类型转换模式
3. **导入路径需同步更新**：包结构调整后必须全局搜索替换
4. **抽象类不能直接实例化**：反序列化时需要创建匿名子类
5. **客户端/服务端代码都要修复**：共享逻辑在两个位置可能有重复实现
