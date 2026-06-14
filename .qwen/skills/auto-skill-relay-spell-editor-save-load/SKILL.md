---
name: relay-spell-editor-save-load
description: 法术编辑器保存/加载完整实现 - 磁盘序列化、放入检测、ProgramCompiler 修复、C2S 网络同步
source: auto-skill
extracted_at: '2026-06-14T06:15:00.000Z'
---

# 法术编辑器保存/加载完整实现

在 Minecraft 26.1.2 中实现法术编辑器的完整保存/加载功能，包括磁盘序列化、放入检测、以及 ProgramCompiler 的 bug 修复。

## 核心问题与修复

### 问题 1: ProgramCompiler.toNbt() 无条件抛出异常

**Bug 代码**:
```java
public static ListTag toNbt(List<Executable> program) throws CompilationException {
    ListTag listTag = new ListTag();
    for (Executable exec : program) {
        if (exec instanceof Operation op) {
            CompoundTag tag = new CompoundTag();
            op.toNbt(tag);
            listTag.add(tag);
        }
        throw new CompilationException("指令 " + exec + " 不是 Operation 类型"); // ❌ 错误：在 if 外面
    }
    return listTag;
}
```

**修复**:
```java
public static ListTag toNbt(List<Executable> program) throws CompilationException {
    ListTag listTag = new ListTag();
    for (Executable exec : program) {
        if (exec instanceof Operation op) {
            CompoundTag tag = new CompoundTag();
            op.toNbt(tag);
            listTag.add(tag);
        } else {
            throw new CompilationException("指令 " + exec + " 不是 Operation 类型"); // ✅ 正确：在 else 块中
        }
    }
    return listTag;
}
```

**Why**: `throw` 语句在 `if` 块外面，导致每次循环都会无条件抛出异常，无论 `exec` 是否是 `Operation` 类型。

### 问题 2: SpellDiskItem 序列化逻辑不完整

**修复后的完整实现**:

```java
public class SpellDiskItem extends Item {

    /**
     * 从磁盘读取程序
     */
    public static List<Executable> getProgram(ItemStack stack) {
        CompoundTag programTag = stack.get(RelayDataComponents.SPELL_PROGRAM);
        if (programTag == null) {
            return List.of();
        }
        Optional<ListTag> listOpt = programTag.getList("program");
        if (listOpt.isEmpty()) {
            return List.of();
        }
        ListTag listTag = listOpt.get();
        try {
            return ProgramCompiler.fromNbt(listTag);
        } catch (ProgramCompiler.CompilationException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * 保存程序到磁盘
     */
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
        } catch (ProgramCompiler.CompilationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从状态机保存状态
     */
    public static void saveFromStateMachine(ItemStack stack, StateMachine machine) {
        List<Executable> program = new ArrayList<>(machine.getProgramStackSnapshot());
        Collections.reverse(program); // 恢复原始顺序
        setProgram(stack, program);
    }

    /**
     * 加载状态到状态机
     */
    public static void loadToStateMachine(ItemStack stack, StateMachine machine) {
        List<Executable> program = getProgram(stack);
        if (!program.isEmpty()) {
            machine.loadProgram(program);
        }
    }

    /**
     * 检查磁盘是否有程序
     */
    public static boolean hasProgram(ItemStack stack) {
        return stack.has(RelayDataComponents.SPELL_PROGRAM);
    }

    /**
     * 获取程序大小
     */
    public static int getProgramSize(ItemStack stack) {
        CompoundTag programTag = stack.get(RelayDataComponents.SPELL_PROGRAM);
        if (programTag == null) {
            return 0;
        }
        Optional<ListTag> listOpt = programTag.getList("program");
        return listOpt.map(ListTag::size).orElse(0);
    }

    /**
     * 清空磁盘
     */
    public static void clear(ItemStack stack) {
        stack.remove(RelayDataComponents.SPELL_PROGRAM);
    }
}
```

**关键点**:
1. **空程序处理**: `setProgram()` 在程序为空时调用 `stack.remove()` 移除组件
2. **异常处理**: 捕获 `CompilationException` 并打印堆栈，避免崩溃
3. **CompoundTag 包装**: 使用 `CompoundTag` 包装 `ListTag`，便于未来扩展

### 问题 3: 放入磁盘时不加载程序

**修复**: 在 `SpellEditorScreenHandler` 中添加磁盘放入检测逻辑。

```java
public class SpellEditorScreenHandler extends AbstractContainerMenu {
    private final List<Executable> program;
    private final SimpleContainer diskContainer;

    public SpellEditorScreenHandler(int syncId, Inventory playerInventory, SpellEditorBlockEntity blockEntity) {
        super(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, syncId);
        this.program = new ArrayList<>();
        
        // 磁盘容器 + 插槽
        this.diskContainer = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
                SpellEditorScreenHandler.this.slotsChanged(this);
            }
        };
        this.addSlot(new Slot(this.diskContainer, 0, DISK_SLOT_X, DISK_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SpellDiskItem;
            }
            
            @Override
            public void onQuickCraft(ItemStack stack, ItemStack previousStack) {
                super.onQuickCraft(stack, previousStack);
                // 磁盘放入时加载程序
                if (!stack.isEmpty() && stack.getItem() instanceof SpellDiskItem) {
                    loadProgramFromDisk();
                }
            }
            
            @Override
            public void set(ItemStack stack) {
                super.set(stack);
                // 磁盘放入时加载程序（包括 Shift 点击放入）
                if (!stack.isEmpty() && stack.getItem() instanceof SpellDiskItem) {
                    loadProgramFromDisk();
                }
            }
        });
        
        // ... 添加玩家物品栏插槽 ...
    }

    /**
     * 从磁盘加载程序到编辑器
     */
    public void loadProgramFromDisk() {
        ItemStack diskStack = getDiskItem();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof SpellDiskItem)) {
            return;
        }
        
        List<Executable> loadedProgram = SpellDiskItem.getProgram(diskStack);
        this.program.clear();
        this.program.addAll(loadedProgram);
    }

    /**
     * 将当前程序保存到磁盘
     */
    public String saveToDisk() {
        ItemStack diskStack = getDiskItem();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof SpellDiskItem)) {
            return "请放入法术磁盘";
        }
        
        SpellDiskItem.setProgram(diskStack, this.program);
        return "已保存 " + this.program.size() + " 条指令";
    }

    public ItemStack getDiskItem() {
        return this.getSlot(DISK_SLOT).getItem();
    }
}
```

**关键点**:
1. **onQuickCraft()**: 检测 Shift 点击放入
2. **set()**: 检测所有放入操作（包括拖拽）
3. **loadProgramFromDisk()**: 清空当前程序并加载磁盘程序

### 问题 4: 客户端编译错误

**修复**: `saveToDisk()` 不再抛出 `CompilationException`，客户端移除不必要的 `try-catch`:

```java
// 客户端 SpellEditorScreen.java
private void onSave() {
    String result = this.menu.saveToDisk();
    saveButton.setMessage(Component.literal(result.length() > 6 ? "已保存" : result));
}
```

### 问题 5: 保存操作在服务端执行（核心问题）

**根本原因**: 物品数据必须在**服务端**才能持久化到存档。客户端直接调用 `SpellDiskItem.setProgram()` 只会修改客户端本地状态，重启后数据丢失。

**修复方案**: 使用 C2S 网络包将保存请求发送到服务端。

#### 5.1 创建网络包

```java
// src/main/java/qdream/relay/networking/payloads/C2S_SaveSpellDiskPayload.java
public record C2S_SaveSpellDiskPayload(CompoundTag programNbt) implements CustomPacketPayload {
    public static final Type<C2S_SaveSpellDiskPayload> TYPE = 
        new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_save_spell_disk"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_SaveSpellDiskPayload> CODEC = 
        StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, C2S_SaveSpellDiskPayload::programNbt,
            C2S_SaveSpellDiskPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

#### 5.2 服务端注册和接收

```java
// src/main/java/qdream/relay/networking/RelayServerNetworking.java
public class RelayServerNetworking {
    public static void register() {
        // 注册包类型
        PayloadTypeRegistry.serverboundPlay().register(
            C2S_SaveSpellDiskPayload.TYPE, 
            C2S_SaveSpellDiskPayload.CODEC
        );

        // 注册接收处理器
        ServerPlayNetworking.registerGlobalReceiver(
            C2S_SaveSpellDiskPayload.TYPE, 
            (payload, context) -> {
                ServerPlayer player = context.player();
                if (player == null) return;

                context.server().execute(() -> {
                    if (player.containerMenu instanceof SpellEditorScreenHandler handler) {
                        ItemStack diskStack = handler.getDiskItem();
                        if (!diskStack.isEmpty() && diskStack.getItem() instanceof SpellDiskItem) {
                            // 在服务端执行保存
                            diskStack.set(RelayDataComponents.SPELL_PROGRAM, payload.programNbt());
                        }
                    }
                });
            }
        );
    }
}
```

#### 5.3 客户端发送请求

```java
// src/client/java/qdream/relay/client/screen/SpellEditorScreen.java
private void onSave() {
    // 将程序序列化为 NBT
    ListTag programList;
    try {
        programList = ProgramCompiler.toNbt(this.menu.getProgramEntries());
    } catch (CompilationException e) {
        saveButton.setMessage(Component.literal("编译错误"));
        e.printStackTrace();
        return;
    }

    // 包装为 CompoundTag 并发送数据包到服务端
    CompoundTag programTag = new CompoundTag();
    programTag.put("program", programList);
    ClientPlayNetworking.send(new C2S_SaveSpellDiskPayload(programTag));
    saveButton.setMessage(Component.literal("已发送保存请求"));
}
```

**关键点**:
1. **客户端序列化**: 客户端将程序序列化为 `ListTag` → 包装为 `CompoundTag`
2. **网络传输**: 通过 `ClientPlayNetworking.send()` 发送到服务端
3. **服务端保存**: 服务端在服务端直接写入 `DataComponent`
4. **持久化**: 服务端数据随物品存档持久化，重启后保留

## 完整使用流程

### 加载流程
```
1. 玩家放入法术磁盘到编辑器插槽
   ↓
2. Slot.set() 或 Slot.onQuickCraft() 被调用
   ↓
3. loadProgramFromDisk() 从磁盘读取程序
   ↓
4. SpellDiskItem.getProgram() 调用 ProgramCompiler.fromNbt()
   ↓
5. 程序加载到编辑器的 program 列表
```

### 保存流程（网络同步）
```
6. 玩家编辑程序（添加/删除操作）
   ↓
7. 玩家点击"保存"按钮
   ↓
8. 客户端序列化程序为 ListTag → CompoundTag
   ↓
9. 发送 C2S_SaveSpellDiskPayload 到服务端
   ↓
10. 服务端接收并写入磁盘的 SPELL_PROGRAM DataComponent
   ↓
11. 数据随物品存档持久化（重启后保留）
```

## 关键注意事项

1. **ProgramCompiler.toNbt() 只能处理 Operation 类型**: 确保程序列表中的所有元素都是 `Operation` 或其子类（如 `Data`）的实例。

2. **DataComponent 网络同步**: `RelayDataComponents.SPELL_PROGRAM` 必须提供 `StreamCodec` 用于网络同步，否则客户端看不到磁盘内容变化。

3. **Slot 检测方法**: 
   - `onQuickCraft()`: 仅检测 Shift 点击放入
   - `set()`: 检测所有放入操作（推荐）
   - `onSwapStack()`: 检测交换操作（可选）

4. **空程序处理**: 保存空程序时应移除 DataComponent，避免存储无意义数据。

5. **异常处理**: 序列化/反序列化失败时打印堆栈并返回空列表，避免游戏崩溃。

## 验证步骤

```bash
# 编译项目
./gradlew build

# 运行客户端测试
./gradlew runClient
```

**测试用例**:
1. 放入空磁盘 → 编辑器显示空程序
2. 添加操作 → 点击保存 → 取出磁盘再放入 → 程序应保留
3. 保存后重启游戏 → 放入磁盘 → 程序应保留
4. 放入已有程序的磁盘 → 编辑器应加载磁盘程序

## 相关文件

- `src/main/java/qdream/relay/mc/ProgramCompiler.java` - 序列化/反序列化核心
- `src/main/java/qdream/relay/items/SpellDiskItem.java` - 磁盘物品操作封装
- `src/main/java/qdream/relay/screen/SpellEditorScreenHandler.java` - 编辑器状态管理
- `src/client/java/qdream/relay/client/screen/SpellEditorScreen.java` - 客户端 GUI
- `src/main/java/qdream/relay/items/RelayDataComponents.java` - DataComponent 注册
- `src/main/java/qdream/relay/networking/payloads/C2S_SaveSpellDiskPayload.java` - C2S 保存请求网络包
- `src/main/java/qdream/relay/networking/RelayServerNetworking.java` - 服务端网络处理
