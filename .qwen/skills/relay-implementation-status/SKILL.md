---
name: relay-implementation-status
description: Relay 模组当前实现状态分析，包括已完成功能、待完成功能、代码质量问题和修复优先级
source: auto-skill
extracted_at: '2026-06-06T00:00:00.000Z'
---

# Relay 模组实现状态分析

基于设计文档 `docs/Relay-概念文档.md` 的当前实现情况分析。

## 一、已完成的核心功能

| 设计文档要求 | 当前实现状态 | 文件位置 | 备注 |
|------------|------------|----------|------|
| **命令系统** | ✅ 完成 | `commands/RelayCommands.java` | `/relay write_spell`, `/relay read`, `/relay clear`, `/relay run` 命令 |
| **Iota 类型系统** | ✅ 完成 | `mc/McIota.java`, `mc/McIotaType.java` | 支持 NUMBER, BOOLEAN, VECTOR, STRING, ENTITY, LIST, NULL |
| **双栈执行模型** | ✅ 完成 | `engine/StateMachine.java` | 维护 `programStack` 和 `dataStack` |
| **操作注册表** | ✅ 完成 | `engine/OperationRegistry.java` | 支持链式 API 注册 |
| **永续运行机制** | ✅ 完成 | `core/ShellTickHandler.java` | 每 tick 驱动状态机 |
| **操作数预算** | ✅ 完成 | `StateMachine.tick(int ops)` | 控制每 tick 执行数量 |
| **反转压入规则** | ✅ 完成 | `EvalOp.java`, `IfOp.java`, `StateMachine.tick()` | 保证从左到右执行顺序 |
| **宽容规则** | ✅ 完成 | `StateMachine.tick()` + `McIota.execute()` | 程序栈弹出数据自动压入数据栈 |
| **事故处理** | ✅ 完成 | `StateMachine.triggerMishap()` | 清空双栈并回调 |
| **通信系统** | ✅ 完成 | `core/CommunicationSystem.java` | 全局 Map，频道容量 1000 |
| **基础操作库** | ✅ 完成 | `operations/base/`, `operations/arithmetic/`, `operations/logic/`, `operations/control/`, `operations/communication/` | Push, Pop, Dup, Swap, Add, Sub, Mul, Div, And, Or, Not, Eq, Lt, Gt, Eval, If, Stop, Send, Recv, Peek |
| **列表操作** | ⚠️ 部分完成 | `operations/list/` | ListGet, ListSet, ListLength, ListAppend |
| **世界交互操作** | ⚠️ 部分完成 | `operations/world/` | GetBlock, PlaceBlock |
| **核心合并逻辑** | ✅ 完成 | `core/CoreGroup.java` | 相邻核心扫描合并 |
| **外壳容器抽象** | ✅ 完成 | `core/ShellContainer.java` | 统一三种外壳接口 |
| **Engine/MC 分层** | ✅ 完成 | `engine/` + `mc/` | 纯 Java 引擎零 MC 依赖 |
| **法术磁盘持久化** | ✅ 完成 | `items/SpellDiskItem.java`, `items/RelayDataComponents.java` | 使用 26.1.2 DataComponent 系统 |

## 二、待完成/简化的功能

| 设计文档要求 | 当前实现状态 | 问题描述 | 优先级 |
|------------|------------|----------|--------|
| **能量管理** | ⚠️ 简化 | `EnergySystem.java` 只提取紫水晶，未集成到状态机执行流程 | P0 |
| **世界交互器检查** | ⚠️ 部分实现 | `requiresWorldInteractor` 已注册，但世界操作需要实际检查 | P1 |
| **interval 可调** | ⚠️ 简化 | `ShellTickHandler.updateCoreState()` 硬编码 interval=1，未从核心读取 | P1 |
| **网络同步** | ❌ 未实现 | 26.1.2 网络 API 变更，`RelayServerNetworking.java` 为空 | P1 |
| **客户端编辑器** | ⚠️ 简化 | `SpellEditorScreen.java` 只有框架，缺少 UI Widget | P2 |
| **实体外壳** | ⚠️ 待完善 | `RelayEntityTypes` 已注册，实体 tick 逻辑待实现 | P1 |
| **工具外壳** | ⚠️ 待完善 | `ToolShellItem.java` 已创建，tick 激活逻辑待实现 | P1 |
| **能量预检** | ❌ 未实现 | 设计文档要求"能量不足时跳过整个 batch" | P0 |
| **操作签名类型推导** | ⚠️ 未使用 | `OperationSignature` 已创建，但编辑器未使用 | P2 |
| **BlockEntity NBT 持久化** | ❌ 未实现 | `ShellBlockEntity` 未覆盖 `saveAdditional/loadAdditional` | P0 |

## 三、代码质量问题

### 1. StateMachine 类型判断逻辑重复

**问题**:
```java
if ("string".equals(top.getType())) { ... }
else if ("list".equals(top.getType())) { ... }
else if ("null".equals(top.getType()) || "number".equals(top.getType()) || ...) { ... }
```

**建议**: 使用 `instanceof` 模式匹配或提取类型判断方法

### 2. ShellTickHandler 与 CoreGroup 脱节

**问题**: `CoreGroup` 实现了完整的合并逻辑，但 `ShellTickHandler.updateCoreState()` 只简单设置 `coreCount=1`

```java
// ShellTickHandler.java
private void updateCoreState(ShellContainer container) {
    ItemStack coreStack = container.getCoreStack();
    if (!coreStack.isEmpty()) {
        coreCount = 1;  // 简化实现
        interval = 1;
    }
}
```

**建议**: 在 tick 开始时调用 `CoreGroup.fromWorld()` 扫描相邻核心

### 3. McIota 的 execute 方法冗余

**问题**: `McIota.execute()` 只是将自己压入数据栈，与 `StateMachine.tick()` 的宽容规则重复

```java
@Override
public void execute(StateMachine executor) {
    if (type != McIotaType.STRING && type != McIotaType.LIST) {
        executor.pushData(this);  // 与 StateMachine 逻辑重复
    }
}
```

**建议**: 移除 `McIota.execute()`，完全由 `StateMachine` 处理

### 4. PushOp 设计矛盾

**问题**: `PushOp.execute()` 是空实现，注释说"由 StateMachine 处理"，但注册表中仍注册为操作

```java
// PushOp.java
@Override
public void execute(StateMachine executor) {
    // Push 操作实际上由 StateMachine 的宽容规则处理
    // 这里不需要额外逻辑
}
```

**建议**: 移除 `PushOp` 从注册表，或在编辑器中作为特殊节点处理

### 5. 操作参数检查不一致

**问题**: 部分操作返回时不抛出异常，部分抛出 `IllegalArgumentException`

```java
// AddOp - 抛出异常
if (!a.isNumber() || !b.isNumber()) {
    throw new IllegalArgumentException("Add 需要两个数值参数");
}

// RecvOp - 静默返回
IData channelData = executor.popData();
if (!(channelData instanceof McIota channel)) return;
```

**建议**: 统一错误处理策略，参数不足/类型不匹配应触发事故

## 四、修复优先级

### P0 - 影响核心功能（必须修复）

1. **能量系统集成到 tick 流程**
   - 文件：`core/ShellTickHandler.java`, `engine/StateMachine.java`
   - 任务：执行前检查能量，不足时跳过 batch
   - 设计：参考文档"能量预检"规则

2. **BlockEntity NBT 持久化**
   - 文件：`blocks/entity/ShellBlockEntity.java`
   - 任务：实现 `saveAdditional(CompoundTag, HolderLookup.Provider)` 和 `loadAdditional()`
   - 依赖：26.1.2 ValueInput/ValueOutput API

### P1 - 完善体验（重要功能）

1. **网络 API 适配 26.1.2**
   - 文件：`networking/RelayServerNetworking.java`, `networking/payloads/`
   - 任务：使用新的 PayloadTypeRegistry API

2. **客户端编辑器 UI 实现**
   - 文件：`client/editor/SpellEditorScreen.java`
   - 任务：添加操作列表、程序编辑区、类型推导提示

3. **实体/工具外壳 tick 逻辑**
   - 文件：`entities/EntityShell.java`, `items/ToolShellItem.java`
   - 任务：实现 `tick()` 方法调用状态机

4. **世界交互器实际检查**
   - 文件：`operations/world/*.java`
   - 任务：在 `execute()` 中检查 `executor.hasWorldInteractor()`

5. **CoreGroup 与 ShellTickHandler 集成**
   - 文件：`core/ShellTickHandler.java`
   - 任务：tick 时调用 `CoreGroup.fromWorld()` 更新核心状态

### P2 - 优化（可延后）

1. **代码重构**
   - StateMachine 类型判断逻辑提取
   - 移除 McIota.execute() 冗余
   - 统一操作错误处理策略

2. **操作签名在编辑器中的使用**
   - 文件：`client/editor/SpellEditorScreen.java`
   - 任务：使用 `OperationSignature` 进行类型推导和验证

3. **单元测试编写**
   - 测试 StateMachine 执行逻辑
   - 测试通信系统
   - 测试操作正确性

## 五、验证清单

修复完成后，运行以下命令验证：

```bash
./gradlew build
./gradlew runClient
```

**成功标志**:
- 无编译错误
- 物品/方块纹理正确显示（非紫黑块）
- 外壳 GUI 可打开
- 状态机可执行程序
- 通信系统可跨外壳传递数据
