---
name: relay-spell-editor-persistence
description: 法术编辑器持久化与客户端同步完整实现 - BlockEntity 序列化、DataComponent 存储、GUI 动态更新
source: auto-skill
extracted_at: '2026-06-14T06:12:27.587Z'
---

# 法术编辑器持久化与客户端同步完整实现

## 核心问题

在 Minecraft 26.1.2 中实现法术编辑器的磁盘持久化和程序加载，需要解决以下关键问题：

1. **BlockEntity 序列化** - 游戏重启后磁盘物品不丢失
2. **客户端/服务端同步** - 双方各有独立的 program 列表
3. **GUI 动态更新** - Widget 需要获取最新的程序列表
4. **DataComponent 系统** - 26.1.2 的物品数据存储方式

## 完整解决方案

### 1. BlockEntity 实现完整序列化（参考 ShellBlockEntity）

```java
public class SpellEditorBlockEntity extends BlockEntity implements MenuProvider, Container {
    // 使用 NonNullList 存储物品栏
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    // ========== Container 接口（必需，供 Slot 使用） ==========
    @Override
    public int getContainerSize() { return inventory.size(); }
    
    @Override
    public ItemStack getItem(int slot) { return inventory.get(slot); }
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        setChanged();
    }
    
    @Override
    public boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this;
    }
    
    // ... 其他 Container 方法（removeItem, clearContent 等）

    // ========== 26.1.2 NBT 序列化（使用 ValueInput/ValueOutput） ==========
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // 使用 ContainerHelper 处理 DataComponent 系统
        ContainerHelper.saveAllItems(output, this.inventory);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.inventory);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }
}
```

**关键点**：
- 实现 `Container` 接口，Slot 才能绑定到 BlockEntity
- 使用 `ContainerHelper.saveAllItems/loadAllItems` 处理 DataComponent
- `stillValid()` 检查 BlockEntity 是否有效

### 2. SpellDiskItem 使用 DataComponent 存储程序

```java
public class SpellDiskItem extends Item {
    // 保存程序到磁盘
    public static void setProgram(ItemStack stack, List<Executable> program) {
        if (program.isEmpty()) {
            stack.remove(RelayDataComponents.SPELL_PROGRAM);
            return;
        }
        
        CompoundTag programTag = new CompoundTag();
        try {
            ListTag listTag = ProgramCompiler.toNbt(program);
            programTag.put("program", listTag);
            stack.set(RelayDataComponents.SPELL_PROGRAM, programTag);
        } catch (CompilationException e) {
            e.printStackTrace();
        }
    }

    // 从磁盘读取程序
    public static List<Executable> getProgram(ItemStack stack) {
        CompoundTag programTag = stack.get(RelayDataComponents.SPELL_PROGRAM);
        if (programTag == null) {
            return List.of();
        }
        Optional<ListTag> listOpt = programTag.getList("program");
        if (listOpt.isEmpty()) {
            return List.of();
        }
        try {
            return ProgramCompiler.fromNbt(listOpt.get());
        } catch (CompilationException e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
```

### 3. SpellEditorScreenHandler 加载程序

```java
public class SpellEditorScreenHandler extends AbstractContainerMenu {
    private final List<Executable> program = new ArrayList<>();
    private final SpellEditorBlockEntity blockEntity;

    public SpellEditorScreenHandler(int syncId, Inventory playerInventory, SpellEditorBlockEntity blockEntity) {
        super(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;

        // 使用 BlockEntity 的物品栏
        if (blockEntity != null) {
            this.addSlot(new Slot(blockEntity, 0, DISK_SLOT_X, DISK_SLOT_Y) {
                @Override
                public void onQuickCraft(ItemStack stack, ItemStack previousStack) {
                    super.onQuickCraft(stack, previousStack);
                    if (!stack.isEmpty() && stack.getItem() instanceof SpellDiskItem) {
                        loadProgramFromDisk(stack);
                    }
                }

                @Override
                public void set(ItemStack stack) {
                    super.set(stack);
                    if (!stack.isEmpty() && stack.getItem() instanceof SpellDiskItem) {
                        loadProgramFromDisk(stack);
                    }
                }
            });
            
            // 打开 GUI 时，如果已有磁盘则加载程序
            ItemStack existingDisk = blockEntity.getDiskStack();
            if (!existingDisk.isEmpty() && existingDisk.getItem() instanceof SpellDiskItem) {
                loadProgramFromDisk(existingDisk);
            }
        }
    }

    // 从磁盘加载程序到编辑器
    public void loadProgramFromDisk(ItemStack diskStack) {
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof SpellDiskItem)) {
            return;
        }
        List<Executable> loadedProgram = SpellDiskItem.getProgram(diskStack);
        this.program.clear();
        this.program.addAll(loadedProgram);
    }
}
```

**关键点**：
- 打开 GUI 时主动检查并加载已有磁盘的程序
- 磁盘放入时（`set()` 或 `onQuickCraft()`）触发加载
- 直接传入 `ItemStack` 避免重复调用 `getDiskItem()`

### 4. ProgramListWidget 使用 Supplier 动态获取列表

```java
public class ProgramListWidget extends AbstractWidget {
    private final Supplier<List<Executable>> programSupplier;

    public ProgramListWidget(int x, int y, int width, int height, Font font, 
                             Supplier<List<Executable>> programSupplier) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.programSupplier = programSupplier;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, ...) {
        // 每次渲染时获取最新列表
        List<Executable> currentProgram = programSupplier.get();
        
        for (int i = 0; i < visibleLines && (i + scrollOffset) < currentProgram.size(); i++) {
            Executable entry = currentProgram.get(dataIndex);
            // ... 渲染逻辑
        }
    }
}
```

**SpellEditorScreen 使用方法引用**：
```java
programListWidget = new ProgramListWidget(
    x, y, width, height, font, 
    this.menu::getProgramEntries  // 方法引用，每次渲染时调用
);
```

**为什么需要 Supplier**：
- 客户端和服务端各有独立的 `program` 列表
- `loadProgramFromDisk()` 修改的是服务端列表
- 客户端 GUI 在 `init()` 时创建 Widget，此时列表可能为空
- 使用 Supplier 确保每次渲染都获取最新数据

### 5. 网络包同步保存请求

**客户端发送保存请求**：
```java
// SpellEditorScreen.java
private void onSave() {
    ListTag programList;
    try {
        programList = ProgramCompiler.toNbt(this.menu.getProgramEntries());
    } catch (CompilationException e) {
        saveButton.setMessage(Component.literal("编译错误"));
        return;
    }

    CompoundTag programTag = new CompoundTag();
    programTag.put("program", programList);
    ClientPlayNetworking.send(new C2S_SaveSpellDiskPayload(programTag));
    saveButton.setMessage(Component.literal("已发送保存请求"));
}
```

**服务端接收并保存**：
```java
// RelayServerNetworking.java
ServerPlayNetworking.registerGlobalReceiver(C2S_SaveSpellDiskPayload.TYPE, (payload, context) -> {
    ServerPlayer player = context.player();
    if (player == null) return;

    context.server().execute(() -> {
        if (player.containerMenu instanceof SpellEditorScreenHandler handler) {
            ItemStack diskStack = handler.getDiskItem();
            if (!diskStack.isEmpty() && diskStack.getItem() instanceof SpellDiskItem) {
                diskStack.set(RelayDataComponents.SPELL_PROGRAM, payload.programNbt());
            }
        }
    });
});
```

## 完整工作流程

```
1. 玩家右键法术编辑器方块
   ↓
2. SpellEditorBlockEntity.createMenu() 创建 SpellEditorScreenHandler
   ↓
3. 构造函数检查 blockEntity.getDiskStack()
   - 如果有磁盘 → loadProgramFromDisk(existingDisk)
   ↓
4. 客户端创建 SpellEditorScreen
   - ProgramListWidget 使用 menu::getProgramEntries Supplier
   ↓
5. 每次渲染时，Supplier 获取最新的 program 列表
   ↓
6. 玩家点击保存
   - 客户端序列化程序 → 发送 C2S_SaveSpellDiskPayload
   - 服务端接收 → 写入磁盘的 DataComponent
   ↓
7. 游戏重启
   - SpellEditorBlockEntity.loadAdditional() 从 NBT 加载物品栏
   - 磁盘物品保留，程序数据保留
```

## 关键设计决策

### 1. 为什么不直接同步 program 列表？
- 客户端也可以读取磁盘的 DataComponent
- 让客户端自己从磁盘读取更简单
- 只需要在打开 GUI 和放入磁盘时加载

### 2. 为什么使用 Supplier 而不是网络同步？
- Supplier 更简单，不需要额外的 S2C 包
- 服务端加载后，客户端下次渲染自然获取到数据
- 减少网络流量和复杂性

### 3. 为什么 BlockEntity 要实现 Container？
- Slot 构造函数需要 Container 参数
- 实现 Container 后 Slot 可以直接绑定到 BlockEntity
- 物品移动、序列化都由 Minecraft 自动处理

## 常见问题排查

### 问题：磁盘在重启后消失
**原因**：BlockEntity 没有实现 `saveAdditional/loadAdditional`
**解决**：使用 `ContainerHelper.saveAllItems/loadAllItems`

### 问题：GUI 显示空列表
**原因**：Widget 持有旧的列表引用
**解决**：使用 Supplier 动态获取最新列表

### 问题：放入磁盘不加载程序
**原因**：没有在 `set()` 或 `onQuickCraft()` 中调用加载方法
**解决**：在 Slot 回调中添加 `loadProgramFromDisk(stack)`

### 问题：打开 GUI 时已有磁盘但不显示
**原因**：没有在构造函数中主动检查
**解决**：添加 `if (!existingDisk.isEmpty()) loadProgramFromDisk(existingDisk)`

### 问题：客户端网络注册报错 "no payload type has been registered"
**原因**：S2C 包类型需要在服务端使用 `PayloadTypeRegistry.clientboundPlay()` 注册
**解决**：
```java
// 服务端注册 S2C 包类型
PayloadTypeRegistry.clientboundPlay().register(S2C_SyncSpellDiskPayload.TYPE, CODEC);

// 客户端只需注册接收器
ClientPlayNetworking.registerGlobalReceiver(S2C_SyncSpellDiskPayload.TYPE, handler);
```

## 26.1.2 网络包注册规则

| 包类型 | 注册位置 | 注册方法 |
|--------|----------|----------|
| C2S (客户端→服务端) | 服务端 | `PayloadTypeRegistry.serverboundPlay().register()` |
| S2C (服务端→客户端) | 服务端 | `PayloadTypeRegistry.clientboundPlay().register()` |
| C2S 接收器 | 服务端 | `ServerPlayNetworking.registerGlobalReceiver()` |
| S2C 接收器 | 客户端 | `ClientPlayNetworking.registerGlobalReceiver()` |

**重要**：包类型必须在服务端注册，客户端只需要注册接收器。

## 相关文件

- `SpellEditorBlockEntity.java` - 方块实体，实现 Container 和序列化
- `SpellEditorScreenHandler.java` - 容器，管理程序列表和加载逻辑
- `SpellDiskItem.java` - 磁盘物品，DataComponent 读写
- `ProgramListWidget.java` - GUI Widget，使用 Supplier 动态获取列表
- `SpellEditorScreen.java` - 客户端 GUI，使用方法引用
- `C2S_SaveSpellDiskPayload.java` - 保存请求网络包
- `RelayServerNetworking.java` - 服务端网络处理
