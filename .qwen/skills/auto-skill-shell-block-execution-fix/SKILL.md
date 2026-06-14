---
name: shell-block-execution-fix
description: Relay 模组 ShellBlock 无法运行程序的调试与修复方案
source: auto-skill
extracted_at: '2026-06-14T04:42:38.884Z'
---

# ShellBlock 无法运行程序的调试与修复

当 ShellBlock 的运行按钮已开启但程序无法执行时，按以下步骤排查和修复。

## 问题诊断流程

### 1. 检查 ShellBlockEntity 是否注册到 ShellRegistry

**症状**: 方块放置后不会被 tick 处理器追踪

**修复**: 在 `ShellBlockEntity` 构造函数中注册，在 `setRemoved()` 中注销：

```java
public ShellBlockEntity(BlockPos pos, BlockState state) {
    super(RelayBlockEntities.SHELL_BLOCK_ENTITY, pos, state);
    // ... 初始化代码 ...
    
    // 注册到 ShellRegistry（仅服务端）
    if (level != null && !level.isClientSide()) {
        ShellRegistry.register(this, pos);
    }
}

@Override
public void setRemoved() {
    super.setRemoved();
    // 从 ShellRegistry 注销
    ShellRegistry.unregister(this);
}
```

### 2. 检查 initialized 状态是否正确重置

**症状**: 更换法术磁盘后程序不重新加载

**原因**: `initialized` 一旦设置为 `true` 就永远不会重置

**修复**: 在 `ShellTickHandler.tick()` 中检测磁盘变更：

```java
// 记录上一个磁盘用于检测变更
private ItemStack lastDisk = ItemStack.EMPTY;

public void tick(ShellContainer container) {
    // ... 其他检查 ...
    
    // 检查磁盘是否变更，如果变更则重置初始化状态
    if (initialized && !container.getDiskStack().equals(lastDisk)) {
        initialized = false;
        container.setInitialized(false);
        container.getStateMachine().clear();
    }
    lastDisk = container.getDiskStack().copy();
    
    // 尝试初始化
    if (!initialized) {
        tryInitialize(container);
    }
    
    // ... 执行逻辑 ...
}
```

### 3. 检查空程序处理逻辑

**症状**: 即使磁盘没有程序，`initialized` 也被设置为 `true`

**修复**: 只有成功加载非空程序后才设置 `initialized = true`：

```java
private void tryInitialize(ShellContainer container) {
    ItemStack diskStack = container.getDiskStack();
    if (!diskStack.isEmpty()) {
        CompoundTag compoundTag = diskStack.get(RelayDataComponents.SPELL_PROGRAM);
        if (compoundTag != null) {
            ListTag programTag = compoundTag.getList("program").orElse(null);
            // ✅ 检查程序不为空
            if (programTag != null && !programTag.isEmpty()) {
                List<Executable> fromNbt;
                try {
                    fromNbt = ProgramCompiler.fromNbt(programTag);
                } catch (CompilationException e) {
                    fromNbt = List.of();
                    e.printStackTrace();
                }
                container.getStateMachine().loadProgram(fromNbt);
                // ✅ 只有加载成功后才设置
                container.setInitialized(true);
                initialized = true;
            }
        }
    }
}
```

### 4. 检查执行循环条件

**症状**: 即使有程序也不执行

**修复**: 添加 `isRunning()` 检查：

```java
// 执行状态机
if (initialized && coreCount > 0 && container.getStateMachine().isRunning()) {
    tickCounter++;
    if (tickCounter >= interval) {
        tickCounter = 0;
        container.getStateMachine().run(coreCount);
        container.setChanged();
    }
}
```

### 5. 为 StateMachine 添加 clear() 方法

**用途**: 重置状态机用于磁盘切换

```java
/**
 * 清空状态机（用于重置）
 */
public void clear() {
    programStack.clear();
    dataStack.clear();
    remainingOps = 0;
}
```

## 完整修复清单

- [ ] `ShellBlockEntity` 构造函数中调用 `ShellRegistry.register()`
- [ ] `ShellBlockEntity.setRemoved()` 中调用 `ShellRegistry.unregister()`
- [ ] `ShellTickHandler` 添加 `lastDisk` 字段检测磁盘变更
- [ ] `ShellTickHandler.tryInitialize()` 检查程序不为空
- [ ] `ShellTickHandler.tick()` 添加 `isRunning()` 检查
- [ ] `StateMachine` 添加 `clear()` 方法

## 验证步骤

1. 放置外壳方块
2. 放入运算核心（槽位 0）
3. 放入包含程序的法术磁盘（槽位 1）
4. 点击运行按钮
5. 观察日志输出或状态机执行情况
6. 更换法术磁盘，确认程序重新加载
