---
name: relay-editor-program-loading
description: Relay 模组法术编辑器程序加载同步模式 - 客户端主动请求 + 服务端懒加载 + 磁盘变化推送
source: auto-skill
extracted_at: '2026-06-14T11:40:37.553Z'
---

# Relay 法术编辑器程序加载同步模式

## 问题背景

法术编辑器需要显示磁盘中的程序列表，但存在以下挑战：
1. **服务端权威**：程序数据存储在服务端 `SpellEditorScreenHandler.program`
2. **客户端渲染**：`ProgramListWidget` 在客户端渲染程序列表
3. **状态同步**：需要确保客户端始终显示最新数据
4. **磁盘变化**：放入/取出磁盘时需要及时更新

## 错误方案（已验证不可行）

### 方案 1：构造函数自动加载
```java
// ❌ 问题：每次打开编辑器都创建新 Handler 实例，program 字段不共享
public SpellEditorScreenHandler(...) {
    if (blockEntity != null) {
        ItemStack existingDisk = blockEntity.getItem(0);
        if (!existingDisk.isEmpty()) {
            loadProgramFromDisk(existingDisk); // 只在该 Handler 生命周期内有效
        }
    }
}
```

### 方案 2：复杂的多重触发机制
```java
// ❌ 问题：onQuickCraft、set()、构造函数都调用 loadProgramFromDisk()
// 导致重复调用、逻辑混乱、难以调试
```

### 方案 3：全局静态引用追踪
```java
// ❌ 问题：使用 static currentInstance 追踪 Screen
// 生命周期管理复杂，容易内存泄漏
```

## 正确方案：客户端主导 + 服务端懒加载

### 核心设计原则

1. **客户端主导**：客户端在关键时机主动请求程序列表
2. **服务端响应**：服务端收到请求时，如果程序为空则懒加载磁盘
3. **事件推送**：磁盘变化时服务端主动推送更新
4. **无状态**：不依赖 Handler 生命周期，每次打开都获取最新数据

### 触发时机

| 时机 | 触发方 | 动作 |
|------|--------|------|
| **打开编辑器** | 客户端 | `init()` 发送 `C2S_RequestProgramPayload` |
| **磁盘放入/取出** | 服务端 | Slot 检测变化 → `loadProgramFromDisk()` → `syncProgramToClient()` |
| **保存程序** | 客户端 | 发送 `C2S_SaveSpellDiskPayload` → 服务端返回 `S2C_SyncSpellDiskPayload` |

### 实现细节

#### 1. 客户端：打开时请求

```java
// SpellEditorScreen.java
@Override
protected void init() {
    super.init();
    
    // 注册接收器
    ClientPlayNetworking.registerReceiver(S2C_SyncSpellDiskPayload.TYPE, (payload, context) -> {
        List<Executable> program = ProgramCompiler.fromNbt(payload.programNbt());
        context.client().execute(() -> {
            clientProgramCache.clear();
            clientProgramCache.addAll(program);
            programListWidget.setProgram(program);
        });
    });
    
    // 打开时请求程序列表
    ClientPlayNetworking.send(new C2S_RequestProgramPayload());
}
```

#### 2. 服务端：懒加载响应

```java
// RelayServerNetworking.java
ServerPlayNetworking.registerGlobalReceiver(C2S_RequestProgramPayload.TYPE, (payload, context) -> {
    context.server().execute(() -> {
        if (player.containerMenu instanceof SpellEditorScreenHandler handler) {
            // 懒加载：如果 program 为空但磁盘存在，先加载
            ItemStack diskStack = handler.getDiskItem();
            if (handler.getProgramEntries().isEmpty() 
                && !diskStack.isEmpty() 
                && diskStack.getItem() instanceof SpellDiskItem) {
                handler.loadProgramFromDisk(diskStack);
            }
            
            // 返回程序列表
            ListTag programList = ProgramCompiler.toNbt(handler.getProgramEntries());
            ServerPlayNetworking.send(player, new S2C_SyncSpellDiskPayload(programTag));
        }
    });
});
```

#### 3. 服务端：磁盘变化推送

```java
// SpellEditorScreenHandler.java
this.addSlot(new Slot(blockEntity, 0, DISK_SLOT_X, DISK_SLOT_Y) {
    @Override
    public void onQuickCraft(ItemStack stack, ItemStack previousStack) {
        super.onQuickCraft(stack, previousStack);
        if (!stack.isEmpty() && stack.getItem() instanceof SpellDiskItem) {
            loadProgramFromDisk(stack); // 自动同步到客户端
        }
    }
    
    @Override
    public void set(ItemStack stack) {
        super.set(stack);
        if (!stack.isEmpty() && stack.getItem() instanceof SpellDiskItem) {
            loadProgramFromDisk(stack); // 自动同步到客户端
        }
    }
});
```

#### 4. 客户端：本地缓存

```java
// SpellEditorScreen.java
private List<Executable> clientProgramCache = new ArrayList<>();

// 接收服务端同步包时更新
public void updateProgramFromServer(List<Executable> program) {
    this.clientProgramCache.clear();
    this.clientProgramCache.addAll(program);
    programListWidget.setProgram(program);
}
```

### 数据流图

```
┌─────────────────────────────────────────────────────────┐
│  服务端                                                   │
│  ┌──────────────────────┐                               │
│  │ SpellEditorBlockEntity│                               │
│  │   └─ getItem(0)      │                               │
│  │        ↓              │                               │
│  │ loadProgramFromDisk() │←── Slot.onQuickCraft/set()   │
│  │        ↓              │                               │
│  │ syncProgramToClient() │                               │
│  └──────────────────────┘                               │
│           ↓ S2C_SyncSpellDiskPayload                    │
└─────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────┐
│  客户端                                                   │
│  ┌──────────────────────┐                               │
│  │ C2S_RequestProgram   │←── SpellEditorScreen.init()  │
│  └──────────────────────┘                               │
│           ↓                                              │
│  ┌──────────────────────┐                               │
│  │ S2C 接收器            │                               │
│  │   └─ clientProgramCache                              │
│  │        ↓              │                               │
│  │ programListWidget    │                               │
│  └──────────────────────┘                               │
└─────────────────────────────────────────────────────────┘
```

## 关键要点

1. **每次打开都请求**：客户端 `init()` 总是发送 `C2S_RequestProgramPayload`，确保获取最新数据
2. **懒加载保护**：服务端只在 `program.isEmpty()` 时加载，避免重复操作
3. **Slot 双重检测**：`onQuickCraft()` 和 `set()` 都调用 `loadProgramFromDisk()`，覆盖不同触发场景
4. **客户端缓存**：`clientProgramCache` 存储当前程序列表，避免频繁网络请求
5. **无状态设计**：不依赖 Handler 或 Screen 的生命周期，每次交互都是独立的

## 适用场景

此模式适用于任何需要**服务端权威数据 + 客户端渲染**的 GUI 场景：
- 物品栏内容同步
- 容器状态显示
- 需要实时反映服务端状态变化的界面

## 相关文件

- `src/main/java/qdream/relay/screen/SpellEditorScreenHandler.java` - 服务端 Handler
- `src/client/java/qdream/relay/client/screen/SpellEditorScreen.java` - 客户端 Screen
- `src/main/java/qdream/relay/networking/RelayServerNetworking.java` - 网络包处理器
- `src/client/java/qdream/relay/client/screen/widget/editor/ProgramListWidget.java` - 程序列表渲染
