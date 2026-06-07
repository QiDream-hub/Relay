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
| **Iota 类型系统** | ✅ 完成 | `types/*.java` | 支持 NUMBER, BOOLEAN, VECTOR, STRING, ENTITY, LIST, NULL |
| **双栈执行模型** | ✅ 完成 | `engine/StateMachine.java` | 维护 `programStack` 和 `dataStack` |
| **操作注册表** | ✅ 完成 | `mc/OperationRegistry.java` | 支持链式 API 注册 |
| **永续运行机制** | ✅ 完成 | `core/ShellTickHandler.java` | 每 tick 驱动状态机 |
| **操作数预算** | ✅ 完成 | `StateMachine.tick(int ops)` | 控制每 tick 执行数量 |
| **反转压入规则** | ✅ 完成 | `EvalOp.java`, `IfOp.java`, `StateMachine.tick()` | 保证从左到右执行顺序 |
| **宽容规则** | ✅ 完成 | `StateMachine.tick()` + 数据类型 `execute()` | 程序栈弹出数据自动压入数据栈 |
| **事故处理** | ✅ 完成 | `StateMachine.triggerMishap()` | 清空双栈并回调 |
| **通信系统** | ✅ 完成 | `core/CommunicationSystem.java` | 全局 Map，频道容量 1000 |
| **基础操作库** | ✅ 完成 | `operations/` | 21 个操作：Pop, Dup, Swap, Add, Sub, Mul, Div, And, Or, Not, Eq, Lt, Gt, Eval, If, Stop, Send, Recv, Peek, ListAppend, ListGet, ListSet, ListLength |
| **列表操作** | ✅ 完成 | `operations/list/` | ListGet, ListSet, ListLength, ListAppend |
| **核心合并逻辑** | ✅ 完成 | `core/CoreGroup.java` | 相邻核心扫描合并 |
| **外壳容器抽象** | ✅ 完成 | `core/ShellContainer.java` | 统一三种外壳接口 |
| **Engine/MC 分层** | ✅ 完成 | `engine/` + `mc/` | 纯 Java 引擎零 MC 依赖 |
| **法术磁盘持久化** | ✅ 完成 | `items/SpellDiskItem.java`, `items/RelayDataComponents.java` | 使用 26.1.2 DataComponent 系统 |
| **BlockEntity NBT 持久化** | ✅ 完成 | `blocks/entity/ShellBlockEntity.java` | 使用 26.1.2 ValueInput/ValueOutput API |
| **状态机序列化** | ✅ 完成 | `mc/StateMachineNbtSerializer.java` | 序列化双栈状态到 NBT |
| **GUI 系统** | ✅ 完成 | `screen/` + `client/editor/` | 外壳 GUI + 法术编辑器 GUI |

## 二、待完成/简化的功能

| 设计文档要求 | 当前实现状态 | 问题描述 | 优先级 |
|------------|------------|----------|--------|
| **能量管理** | ⚠️ 简化 | `EnergySystem.java` 只提取紫水晶，未集成到状态机执行流程 | P0 |
| **世界交互器检查** | ⚠️ 部分实现 | 操作自己在 `execute()` 中检查，但世界操作需要实际实现 | P1 |
| **interval 可调** | ⚠️ 简化 | `ShellTickHandler.updateCoreState()` 硬编码 interval=1，未从核心读取 | P1 |
| **网络同步** | ❌ 未实现 | 26.1.2 网络 API 变更，`RelayServerNetworking.java` 为空 | P1 |
| **实体外壳** | ⚠️ 待完善 | `RelayEntityTypes` 已注册，实体完整逻辑待实现 | P1 |
| **工具外壳** | ⚠️ 待完善 | `ToolShellItem.java` 已创建，tick 激活逻辑待实现 | P1 |
| **能量预检** | ❌ 未实现 | 设计文档要求"能量不足时跳过整个 batch" | P0 |
| **操作签名类型推导** | ⚠️ 未使用 | `OperationSignature` 已创建，但编辑器未使用 | P2 |
| **方块 codec() 方法** | ⚠️ 简化 | `ShellBlock.codec()` 和 `SpellEditorBlock.codec()` 返回 null | P1 |
| **物品栏序列化** | ⚠️ 简化 | 由于 DataComponent 系统，ItemStack 序列化暂时简化 | P2 |
| **法术编辑器鼠标交互** | ⚠️ 简化 | `SpellEditorScreen` 鼠标点击事件处理待完善 | P2 |

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

### P1 - 完善体验（重要功能）

1. **方块 codec() 方法实现**
   - 文件：`blocks/ShellBlock.java`, `blocks/SpellEditorBlock.java`
   - 任务：实现正确的 `MapCodec<? extends BaseEntityBlock>`
   - 影响：方块放置和注册需要

2. **网络 API 适配 26.1.2**
   - 文件：`networking/RelayServerNetworking.java`, `networking/payloads/`
   - 任务：使用新的 PayloadTypeRegistry API

3. **实体/工具外壳 tick 逻辑**
   - 文件：`entities/EntityShell.java`, `items/ToolShellItem.java`
   - 任务：实现完整的 `tick()` 方法调用状态机

4. **CoreGroup 与 ShellTickHandler 集成**
   - 文件：`core/ShellTickHandler.java`
   - 任务：tick 时调用 `CoreGroup.fromWorld()` 更新核心状态

### P2 - 优化（可延后）

1. **物品栏 DataComponent 适配**
   - 文件：`blocks/entity/ShellBlockEntity.java`
   - 任务：使用 DataComponent 系统序列化 ItemStack

2. **法术编辑器鼠标交互完善**
   - 文件：`client/editor/SpellEditorScreen.java`
   - 任务：实现点击添加/删除操作的交互逻辑

3. **代码重构**
   - StateMachine 类型判断逻辑提取
   - 统一操作错误处理策略

4. **操作签名在编辑器中的使用**
   - 文件：`client/editor/SpellEditorScreen.java`
   - 任务：使用 `OperationSignature` 进行类型推导和验证

5. **单元测试编写**
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
- 世界保存/加载后状态正确恢复

## 六、序列化系统架构（2026-06-07 完成）

### 序列化场景

1. **法术磁盘加载/保存程序** ✅
   - 位置：`SpellDiskItem.java`
   - 方式：使用 DataComponent 存储 `CompoundTag`
   - 方法：`getProgram()`, `setProgram()`, `saveFromStateMachine()`, `loadToStateMachine()`

2. **外壳方块实体持久化** ✅
   - 位置：`ShellBlockEntity.java`
   - 方式：使用 26.1.2 的 `ValueInput`/`ValueOutput` 系统
   - 方法：`saveAdditional(ValueOutput)`, `loadAdditional(ValueInput)`, `getUpdateTag()`
   - 保存数据：能量、状态机状态、TickHandler 状态

3. **状态机完整状态序列化** ✅
   - 位置：`StateMachineNbtSerializer.java`
   - 方式：使用 NBT `CompoundTag`
   - 方法：`serialize()`, `deserialize()`
   - 保存数据：程序栈、数据栈、世界交互器状态、栈大小限制

4. **Iota 类型序列化** ✅
   - 位置：各种 Iota 类型（`NumberIota`, `ListIota` 等）
   - 方式：继承 `Data` 基类，实现 `toNbt()`/`fromNbt()` 方法
   - 注册：通过 `OperationRegistry.registerData()` 注册数据类型工厂

### 序列化流程

```
玩家编写程序
    ↓
保存到法术磁盘 (SpellDiskItem.setProgram)
    ↓
[CompoundTag 存储 via DataComponent]
    ↓
外壳方块读取磁盘 (ShellTickHandler.tryInitialize)
    ↓
加载到状态机 (SpellDiskItem.loadToStateMachine)
    ↓
StateMachine.loadProgram(program)
    ↓
世界保存时 (ShellBlockEntity.saveAdditional)
    ↓
[ValueOutput 存储]
    - energy: int
    - stateMachine: CompoundTag (嵌套)
    - tickCounter, coreCount, interval, initialized
    ↓
世界加载时 (ShellBlockEntity.loadAdditional)
    ↓
恢复所有状态
```

### 26.1.2 API 适配要点

1. **ValueInput/ValueOutput** - 位于 `net.minecraft.world.level.storage` 包
2. **CompoundTag getter 返回 Optional** - 使用 `.orElse()` 或 `.isPresent()` 处理
3. **HolderLookup.Provider** - `getUpdateTag()` 方法需要此参数
4. **Codec 系统** - 使用 `CompoundTag.CODEC` 进行复杂类型序列化
