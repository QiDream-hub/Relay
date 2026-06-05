---
name: relay-spell-editor-pattern
description: Relay 法术编辑器实现模式 - ScreenHandler 管理程序状态、临时运行测试、26.1.2 客户端 GUI 适配、方块实现
source: auto-skill
extracted_at: '2026-06-06T00:00:00.000Z'
---

# Relay 法术编辑器实现模式

在 Minecraft 26.1.2 (1.21+) 版本中实现可视化编程编辑器的完整模式，包括 ScreenHandler 状态管理、临时运行测试功能和客户端 GUI 适配。

## 核心架构

编辑器采用三层架构（2026-06-06 更新：改为方块实现）：

```
┌─────────────────────────────────────────────────────────┐
│  SpellEditorBlock (方块)                                 │
│  - 右键打开编辑器菜单                                     │
│  - 继承 BaseEntityBlock                                  │
│  - 使用 MenuProvider 创建菜单                            │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  SpellEditorScreenHandler (共享包)                       │
│  - 管理程序列表 (List<String>)                           │
│  - 管理可用操作列表                                       │
│  - 提供添加/删除/清空方法                                 │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  SpellEditorScreen (客户端)                              │
│  - 渲染操作列表和程序列表                                 │
│  - 处理鼠标点击交互                                       │
│  - 临时运行测试功能                                       │
└─────────────────────────────────────────────────────────┘
```

## 1. ScreenHandler 实现

ScreenHandler 在共享包 (`src/main/java`) 中实现，管理编辑器状态。

```java
package qdream.relay.screen;

public class SpellEditorScreenHandler extends AbstractContainerMenu {
    /** 当前编辑的程序列表（操作 ID 序列） */
    private final List<String> program;

    /** 所有可用的操作 ID */
    private final List<String> availableOperations;

    public SpellEditorScreenHandler(int syncId, Inventory playerInventory) {
        super(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, syncId);
        this.program = new ArrayList<>();
        this.availableOperations = new ArrayList<>(OperationRegistry.getAllIds());

        // 添加玩家物品栏插槽
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 220 + y * 18));
            }
        }
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 278));
        }
    }

    // ========== 程序管理方法 ==========

    public List<String> getProgram() {
        return program;
    }

    public void setProgram(List<String> program) {
        this.program.clear();
        this.program.addAll(program);
    }

    public void addOperation(String opId) {
        if (OperationRegistry.contains(opId)) {
            this.program.add(opId);
        }
    }

    public void removeOperation(int index) {
        if (index >= 0 && index < this.program.size()) {
            this.program.remove(index);
        }
    }

    public void clearProgram() {
        this.program.clear();
    }

    public List<String> getAvailableOperations() {
        return availableOperations;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
```

### MenuType 注册

```java
public class RelayScreenHandlers {
    public static final MenuType<SpellEditorScreenHandler> SPELL_EDITOR_SCREEN_HANDLER;

    static {
        SPELL_EDITOR_SCREEN_HANDLER = new MenuType<>(
            (syncId, inventory) -> new SpellEditorScreenHandler(syncId, inventory),
            FeatureFlags.VANILLA_SET
        );
        Identifier editorId = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "spell_editor");
        Registry.register(BuiltInRegistries.MENU, editorId, SPELL_EDITOR_SCREEN_HANDLER);
    }

    public static void init() {}
}
```

## 2. MenuProvider 实现

创建独立的 MenuProvider 用于打开编辑器（位于 `blocks` 包）：

```java
package qdream.relay.blocks;

public class SpellEditorMenuProvider implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.literal("法术编辑器");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new SpellEditorScreenHandler(syncId, inv);
    }
}
```

## 3. 编辑器方块实现 (2026-06-06 更新)

方块继承 `BaseEntityBlock`，右键打开编辑器菜单：

```java
package qdream.relay.blocks;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;

public class SpellEditorBlock extends BaseEntityBlock {
    public SpellEditorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null; // 简单实现，实际应返回正确的 codec
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // 服务端打开编辑器菜单
        player.openMenu(new SpellEditorMenuProvider());

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null; // 无需方块实体
    }
}
```

### 方块注册

```java
package qdream.relay.blocks;

public class RelayBlocks {
    public static final Block SPELL_EDITOR_BLOCK = register(
        "spell_editor_block", 
        SpellEditorBlock::new,
        BlockBehaviour.Properties.of(), 
        true // 注册物品（BlockItem 自动创建）
    );
}
```

### 创意标签配置

```java
// 在 RelayItems 中，将方块添加到创意标签
.displayItems((params, output) -> {
    // 核心组件
    output.accept(RelayItems.COMPUTING_CORE);
    output.accept(RelayItems.ENERGY_MODULE);
    output.accept(RelayItems.SPELL_DISK);

    // 方块
    output.accept(RelayBlocks.SPELL_EDITOR_BLOCK);
    output.accept(RelayBlocks.SHELL_BLOCK);
    // ...
})
```

## 4. 客户端 Screen 实现

由于 26.1.2 客户端 GUI API 重大变更，使用简化实现：

```java
package qdream.relay.client.editor;

public class SpellEditorScreen extends AbstractContainerScreen {
    private final SpellEditorScreenHandler handler;
    private StateMachine testMachine;
    private int selectedOpIndex = -1;

    public SpellEditorScreen(SpellEditorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, Component.literal("法术编辑器"));
        this.handler = handler;
        this.testMachine = new StateMachine(1024);
    }

    @Override
    protected void init() {
        super.init();

        int left = leftPos;
        int top = topPos;

        // 运行测试按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("运行测试"),
            btn -> onRun()
        ).pos(left + 230, top + 50).size(80, 20).build());

        // 清空程序按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("清空程序"),
            btn -> onClear()
        ).pos(left + 230, top + 80).size(80, 20).build());

        // 删除选中按钮
        this.addRenderableWidget(Button.builder(
            Component.literal("删除选中"),
            btn -> onDelete()
        ).pos(left + 230, top + 110).size(80, 20).build());
    }

    // 鼠标点击处理 - 点击列表区域添加/选择操作
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int left = leftPos;
            int top = topPos;
            int rowHeight = 14;

            // 点击操作列表区域添加操作
            List<String> ops = handler.getAvailableOperations();
            for (int i = 0; i < Math.min(ops.size(), 14); i++) {
                int y = top + 10 + i * rowHeight;
                if (mouseX >= left + 8 && mouseX <= left + 128 &&
                    mouseY >= y && mouseY < y + rowHeight) {
                    handler.addOperation(ops.get(i));
                    return true;
                }
            }

            // 点击程序列表区域选择操作
            List<String> program = handler.getProgram();
            for (int i = 0; i < Math.min(program.size(), 14); i++) {
                int y = top + 10 + i * rowHeight;
                if (mouseX >= left + 138 && mouseX <= left + 258 &&
                    mouseY >= y && mouseY < y + rowHeight) {
                    selectedOpIndex = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ========== 临时运行测试功能 ==========

    private void onRun() {
        List<String> program = handler.getProgram();
        if (program.isEmpty()) return;

        // 将字符串程序转换为 McIota
        List<IExecutable> iotaProgram = new ArrayList<>();
        for (String opId : program) {
            iotaProgram.add(McIota.ofString(opId));
        }

        // 加载到测试状态机
        testMachine = new StateMachine(1024);
        testMachine.setMishapHandler(reason -> {});
        testMachine.loadProgram(iotaProgram);

        // 执行 10 个 tick 用于测试
        for (int tick = 0; tick < 10 && testMachine.isRunning(); tick++) {
            testMachine.tick(10);
        }
    }

    private void onClear() {
        handler.clearProgram();
        selectedOpIndex = -1;
    }

    private void onDelete() {
        if (selectedOpIndex >= 0 && selectedOpIndex < handler.getProgram().size()) {
            handler.removeOperation(selectedOpIndex);
            selectedOpIndex = -1;
        }
    }
}
```

## 5. 客户端注册

```java
public class RelayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RelayScreenHandlers.init();
        RelayEntityRenderers.register();
        RelayClientNetworking.register();
    }
}
```

## 6. 26.1.2 API 适配要点

### 6.1 MenuType 构造函数

```java
// 26.1.2 需要两个参数
new MenuType<>(factory, FeatureFlags.VANILLA_SET)
```

### 6.2 ServerPlayer.openMenu

```java
// 使用 MenuProvider
serverPlayer.openMenu(new SpellEditorMenuProvider());

// 或使用 MenuType + Buffer
serverPlayer.openMenu(
    RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER,
    buf -> {}  // 不需要额外数据
);
```

### 6.3 客户端鼠标事件

26.1.2 的鼠标事件签名可能变化，如果遇到编译错误：
- 移除 `@Override` 注解
- 检查方法签名是否为 `(MouseButtonEvent, boolean)`

### 6.4 Font 渲染

26.1.2 的 Font 渲染 API 变化：
- `font.draw(poseStack, text, x, y, color)` 需要 PoseStack
- 或使用 `font.drawInBatch()` 方法

## 7. 临时运行测试功能设计

### 设计目标

1. **即时反馈**: 玩家点击按钮后立即看到程序执行结果
2. **隔离测试**: 使用独立的状态机，不影响实际游戏状态
3. **有限执行**: 限制执行 tick 数防止卡顿

### 实现模式

```java
// 1. 将程序字符串转换为可执行单元
List<IExecutable> iotaProgram = new ArrayList<>();
for (String opId : program) {
    iotaProgram.add(McIota.ofString(opId));
}

// 2. 创建测试状态机
StateMachine testMachine = new StateMachine(1024);
testMachine.setMishapHandler(reason -> {
    // 处理事故（如类型错误、未知操作等）
});

// 3. 加载程序
testMachine.loadProgram(iotaProgram);

// 4. 执行有限 tick 数
for (int tick = 0; tick < MAX_TICKS && testMachine.isRunning(); tick++) {
    testMachine.tick(OPS_PER_TICK);
}

// 5. 检查结果
List<IData> dataStack = testMachine.getDataStackSnapshot();
// 显示 dataStack 内容给玩家
```

### 状态显示

```java
// 在 Screen 中显示状态
font.draw(poseStack, "操作数：" + program.size(), x, y, 0xAAAAAA);
font.draw(poseStack, "状态：" + (isRunning ? "运行中" : "已停止"), x, y, 0x00FF00);
font.draw(poseStack, "数据栈：" + testMachine.getDataStackSize(), x, y, 0xAAAAAA);

// 显示栈顶元素
List<IData> snapshot = testMachine.getDataStackSnapshot();
for (int i = 0; i < Math.min(3, snapshot.size()); i++) {
    font.draw(poseStack, formatIota(snapshot.get(i)), x, y + i * 12, 0xCCCCCC);
}
```

## 8. 完整使用流程 (2026-06-06 更新：方块实现)

```
1. 玩家在创造模式获取"法术编辑器方块"
   ↓
2. 放置方块后右键点击
   ↓
3. SpellEditorBlock.useWithoutItem() 调用
   ↓
4. player.openMenu(new SpellEditorMenuProvider())
   ↓
5. 客户端打开 SpellEditorScreen
   ↓
6. 玩家点击操作列表添加操作
   ↓
7. handler.addOperation(opId) 更新程序列表
   ↓
8. 玩家点击"运行测试"按钮
   ↓
9. 程序加载到测试状态机执行 10 tick
   ↓
10. 显示数据栈结果
```

### 物品 vs 方块实现对比

| 特性 | 物品实现 | 方块实现 |
|------|----------|----------|
| 激活方式 | 手持右键 | 放置后右键 |
| 基类 | `Item` | `BaseEntityBlock` |
| 交互方法 | `interact()` | `useWithoutItem()` |
| 返回值 | `InteractionResult.SUCCESS` | `InteractionResult.CONSUME` |
| 包位置 | `items` | `blocks` |
| 注册表 | `RelayItems` | `RelayBlocks` |

## 9. 待完善功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 完整可视化渲染 | P1 | 操作列表/程序列表背景和悬停效果 |
| 拖拽排序 | P2 | 支持拖拽调整操作顺序 |
| 类型推导提示 | P2 | 实时显示操作输入/输出类型 |
| 程序保存/加载 | P1 | 保存到法术磁盘 |
| 错误高亮显示 | P2 | 执行错误时高亮问题操作 |
| 单步执行 | P3 | 调试模式 |

## 10. 验证步骤

```bash
# 编译项目
./gradlew build

# 运行客户端
./gradlew runClient
```

成功标志：
- 无编译错误
- 创造模式可获取"法术编辑器"物品
- 右键打开编辑器界面
- 点击操作列表可添加操作
- 点击"运行测试"可执行程序
