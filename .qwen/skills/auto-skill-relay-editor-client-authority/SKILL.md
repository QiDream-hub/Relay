---
name: relay-editor-client-authority
description: Relay 法术编辑器客户端权威架构 - 客户端维护编辑状态，BlockEntity 持久化存储，关闭时自动保存
source: auto-skill
extracted_at: '2026-06-14T12:38:53.797Z'
---

# Relay 法术编辑器客户端权威架构

## 核心设计

1. **客户端**维护程序列表的编辑状态（`clientProgramCache`），所有修改操作先在客户端执行，然后通过网络包同步到服务端
2. **BlockEntity**持久化存储程序列表，不随 GUI 关闭而丢失
3. **服务端**作为最终权威，在关闭 GUI 时自动保存程序到磁盘

## 为什么采用这种架构

**问题背景**：
- 原有架构中，`SpellEditorScreenHandler` 每次打开 GUI 都创建新实例，本地的 `program` 列表无法持久化
- 客户端无法直接获取物品数据（客户端 - 服务端分离）
- 网络同步逻辑复杂，容易出现状态不一致

**解决方案**：
- 客户端作为编辑操作的"权威"，即时响应用户操作
- BlockEntity 作为持久化存储，程序列表随方块存在而存在
- 服务端负责持久化和验证，关闭 GUI 时自动保存

## 数据流

```
打开 GUI
    ↓
SpellEditorScreenHandler 创建
    ↓
blockEntity.loadProgramFromDisk() → 从磁盘加载到 blockEntity.program
    ↓
S2C_SyncSpellDiskPayload → 客户端显示

用户操作 (添加/删除/清空)
    ↓
修改 clientProgramCache (客户端即时响应)
    ↓
发送 C2S_ProgramModifiedPayload
    ↓
blockEntity.setProgram() → 更新 BlockEntity 中的 program

点击保存
    ↓
发送 C2S_SaveSpellDiskPayload
    ↓
blockEntity.saveProgramToDisk() → 写入磁盘物品

关闭 GUI
    ↓
SpellEditorScreenHandler.removed()
    ↓
blockEntity.saveProgramToDisk() → 确保保存到磁盘

BlockEntity 销毁/世界保存
    ↓
saveAdditional() → NBT 持久化
```

## 关键实现

### 1. BlockEntity 持久化存储

```java
// SpellEditorBlockEntity.java
public class SpellEditorBlockEntity extends BlockEntity implements MenuProvider, Container {
    /** 程序列表：持久化存储，不随 GUI 关闭而丢失 */
    private final List<Executable> program = new ArrayList<>();

    public List<Executable> getProgram() { return program; }
    public void setProgram(List<Executable> program) {
        this.program.clear();
        this.program.addAll(program);
        setChanged();
    }

    public void loadProgramFromDisk() {
        ItemStack diskStack = getDiskStack();
        if (!diskStack.isEmpty() && diskStack.getItem() instanceof SpellDiskItem) {
            List<Executable> loaded = SpellDiskItem.getProgram(diskStack);
            setProgram(loaded);
        }
    }

    public void saveProgramToDisk() {
        ItemStack diskStack = getDiskStack();
        if (!diskStack.isEmpty() && diskStack.getItem() instanceof SpellDiskItem) {
            SpellDiskItem.setProgram(diskStack, this.program);
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        ContainerHelper.saveAllItems(output, this.inventory);
        // 程序列表通过 BlockEntity 自动持久化
    }
}
```

### 2. 客户端程序缓存

```java
// SpellEditorScreen.java
private List<Executable> clientProgramCache = new ArrayList<>();

// 所有修改操作先修改缓存，再同步
private void addOperationToClientProgram(String opId) {
    Executable entry = createExecutable(opId);
    this.clientProgramCache.add(entry);
    this.programListWidget.setProgram(this.clientProgramCache);  // 即时更新 UI
    syncProgramToServer();  // 异步同步到服务端
}
```

### 3. ScreenHandler 使用 BlockEntity 的程序列表

```java
// SpellEditorScreenHandler.java
public class SpellEditorScreenHandler extends AbstractContainerMenu {
    private final SpellEditorBlockEntity blockEntity;

    public List<Executable> getProgramEntries() {
        return blockEntity != null ? blockEntity.getProgram() : new ArrayList<>();
    }

    public void onProgramModified(CompoundTag programTag) {
        if (blockEntity == null) return;
        List<Executable> newProgram = ProgramCompiler.fromNbt(programTag.getList("program"));
        blockEntity.setProgram(newProgram);
    }

    public void saveProgramToDisk() {
        if (blockEntity == null) return;
        ItemStack diskStack = getDiskItem();
        if (!diskStack.isEmpty() && diskStack.getItem() instanceof SpellDiskItem) {
            SpellDiskItem.setProgram(diskStack, blockEntity.getProgram());
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (blockEntity != null) {
            blockEntity.saveProgramToDisk();  // 关闭时自动保存
        }
    }
}
```

## 网络包设计

### C2S_ProgramModifiedPayload (客户端 → 服务端)
```java
public record C2S_ProgramModifiedPayload(CompoundTag programNbt) implements CustomPacketPayload {
    // 每次编辑操作后发送
}
```

### C2S_SaveSpellDiskPayload (客户端 → 服务端)
```java
public record C2S_SaveSpellDiskPayload(CompoundTag programNbt) implements CustomPacketPayload {
    // 点击保存按钮时发送
}
```

### S2C_SyncSpellDiskPayload (服务端 → 客户端)
```java
// 打开 GUI 时或保存确认后发送
```

## 优势

| 方面 | 旧架构 | 新架构 |
|------|--------|--------|
| **UI 响应** | 等待服务端确认 | 即时响应 |
| **网络流量** | 每次操作双向同步 | 单向 C2S |
| **状态一致性** | 容易不同步 | 客户端权威，服务端最终一致 |
| **数据持久化** | 手动保存 | 关闭自动保存 |
| **代码复杂度** | 高 | 低 |

## 适用场景

此模式适用于：
- 需要即时 UI 响应的编辑器类 GUI
- 客户端频繁修改、服务端持久化的场景
- 可以接受短暂状态不一致（最终一致）的场景

## 相关文件

- `src/client/java/qdream/relay/client/screen/SpellEditorScreen.java` - 客户端编辑器
- `src/main/java/qdream/relay/screen/SpellEditorScreenHandler.java` - 服务端处理器
- `src/main/java/qdream/relay/networking/payloads/C2S_ProgramModifiedPayload.java` - 程序修改包
- `src/main/java/qdream/relay/networking/RelayServerNetworking.java` - 网络包注册
