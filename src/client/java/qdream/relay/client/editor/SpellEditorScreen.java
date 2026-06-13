package qdream.relay.client.editor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import qdream.relay.screen.SpellEditorScreenHandler;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Operation;

import java.util.ArrayList;
import java.util.List;

/**
 * 栈图编辑器 Screen - 基于 AbstractContainerScreen 实现
 *
 * 布局：
 * [可用操作] | [程序列表] | [数据栈]
 *            | [执行按钮]  | [程序栈]
 */
public class SpellEditorScreen extends AbstractContainerScreen<SpellEditorScreenHandler> {

    private final StateMachine testMachine;

    // 运行状态
    private String lastMishapReason = null;

    // 布局常量
    private static final int OPERATIONS_PANEL_WIDTH = 120;
    private static final int PROGRAM_PANEL_WIDTH = 120;
    private static final int PANEL_PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 370;

    // 列表区域上下边距
    private static final int LIST_TOP_MARGIN = LINE_HEIGHT + 8;  // 标题行 + 间距
    private static final int LIST_BOTTOM_MARGIN = 20;             // 底部提示区高度
    private static final int EDITOR_PANEL_HEIGHT = 260;           // 编辑器面板区域总高度（不含背包）

    // 自定义 Widget（在 init 中创建）
    private OperationListWidget operationListWidget;
    private ProgramListWidget programListWidget;

    public SpellEditorScreen(SpellEditorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.testMachine = new StateMachine(1024);
        this.testMachine.setMishapHandler(reason -> this.lastMishapReason = reason);
    }

    @Override
    protected void init() {
        super.init();

        // 计算列表 Widget 的纵向范围（仅占据编辑器面板区域，不延伸到背包）
        int listTop = this.topPos + LIST_TOP_MARGIN;
        int listHeight = EDITOR_PANEL_HEIGHT - LIST_TOP_MARGIN - LIST_BOTTOM_MARGIN;

        // 可用操作列表 Widget
        operationListWidget = new OperationListWidget(
            this.leftPos + PANEL_PADDING,
            listTop,
            OPERATIONS_PANEL_WIDTH - PANEL_PADDING * 2,
            listHeight,
            this.font,
            this.menu.getAvailableOperations()
        );
        operationListWidget.setOnOperationClicked(opId -> this.menu.addOperation(opId));
        this.addRenderableWidget(operationListWidget);

        // 程序列表 Widget
        programListWidget = new ProgramListWidget(
            this.leftPos + OPERATIONS_PANEL_WIDTH + PANEL_PADDING,
            listTop,
            PROGRAM_PANEL_WIDTH - PANEL_PADDING,
            listHeight,
            this.font,
            this.menu.getProgram()
        );
        this.addRenderableWidget(programListWidget);

        // 控制按钮区域（右侧面板）
        int buttonX = this.leftPos + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING * 3;
        int buttonY = this.topPos + 180;

        // 运行测试按钮
        this.addRenderableWidget(Button.builder(Component.literal("运行"), btn -> onRun())
            .pos(buttonX, buttonY).size(60, 20).build());

        // 清空程序按钮
        this.addRenderableWidget(Button.builder(Component.literal("清空"), btn -> onClear())
            .pos(buttonX, buttonY + 25).size(60, 20).build());

        // 删除选中按钮
        this.addRenderableWidget(Button.builder(Component.literal("删除"), btn -> onDelete())
            .pos(buttonX, buttonY + 50).size(60, 20).build());
        // 将玩家物品栏标题移出可视区域
        this.inventoryLabelY = this.imageHeight + 10;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 渲染深色背景
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF101010);

        // 渲染边框
        graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF404040);

        // 调用父类渲染 Slot 等（也会渲染已注册的 Widget）
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;

        // 渲染分隔线（仅贯穿编辑器面板区域）
        int panelBottom = top + EDITOR_PANEL_HEIGHT;
        graphics.verticalLine(left + OPERATIONS_PANEL_WIDTH, top, panelBottom, 0xFF404040);
        graphics.verticalLine(left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH, top, panelBottom, 0xFF404040);

        // 编辑器面板与背包的分隔线
        graphics.horizontalLine(left, left + this.imageWidth, panelBottom + 4, 0xFF505050);

        // 渲染面板标题（在 Widget 之上）
        graphics.text(this.font, "可用操作", left + PANEL_PADDING, top + 5, 0xFF00FF00);
        graphics.text(this.font, "程序列表", left + OPERATIONS_PANEL_WIDTH + PANEL_PADDING, top + 5, 0xFFFFFF00);
        graphics.text(this.font, "栈视图", left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING * 3, top + 5, 0xFF00FFFF);

        // 渲染栈视图（仍为手绘，不属于列表 Widget）
        renderStacks(graphics, left, top);

        // 渲染事故信息
        if (lastMishapReason != null) {
            int infoX = left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING * 3;
            graphics.text(this.font, "事故：" + lastMishapReason, infoX, this.topPos + 240, 0xFFFF0000);
        }

        // 渲染操作提示（编辑器面板底部）
        int hintY = top + EDITOR_PANEL_HEIGHT - 14;
        graphics.text(this.font, "滚轮滚动 · 点击添加",
            left + PANEL_PADDING, hintY, 0xFF888888);
        graphics.text(this.font, "滚轮滚动 · 点击选中",
            left + OPERATIONS_PANEL_WIDTH + PANEL_PADDING, hintY, 0xFF888888);
    }

    /**
     * 渲染数据栈和程序栈
     */
    private void renderStacks(GuiGraphicsExtractor graphics, int left, int top) {
        int x = left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING * 3;
        int y = top + LINE_HEIGHT + 5;

        // 数据栈标题
        graphics.text(this.font, "数据栈:", x, y, 0xFF00FFFF);
        y += LINE_HEIGHT;

        // 数据栈内容
        List<Executable> dataStack = testMachine.getDataStackSnapshot();
        for (int i = dataStack.size() - 1; i >= 0 && y < top + 100; i--) {
            Executable data = dataStack.get(i);
            String text = ((Operation) data).getId();
            graphics.text(this.font, text, x, y, 0xFF00AAAA);
            y += LINE_HEIGHT;
        }

        // 程序栈标题
        y += 10;
        graphics.text(this.font, "程序栈:", x, y, 0xFFFF8800);
        y += LINE_HEIGHT;

        // 程序栈内容
        List<Executable> programStack = testMachine.getProgramStackSnapshot();
        for (int i = programStack.size() - 1; i >= 0 && y < top + 200; i--) {
            Executable exec = programStack.get(i);
            String text = ((Operation) exec).getId();
            graphics.text(this.font, text, x, y, 0xFFFF8800);
            y += LINE_HEIGHT;
        }
    }

    private void onRun() {
        List<String> program = this.menu.getProgram();
        if (program.isEmpty()) return;

        // 将程序加载到测试状态机
        List<Executable> iotaProgram = new ArrayList<>();
        // for (String opId : program) {
        //     iotaProgram.add(new Operation(opId, 0) {
        //         @Override
        //         public void execute(StateMachine executor) {
        //             // 测试用空实现
        //         }
        //     });
        // }

        testMachine.loadProgram(iotaProgram);
        lastMishapReason = null;

        // 执行 10 个 tick 用于测试
        for (int tick = 0; tick < 10 && testMachine.isRunning(); tick++) {
            testMachine.run(10);
        }
    }

    private void onClear() {
        this.menu.clearProgram();
        programListWidget.clearSelection();
        lastMishapReason = null;
    }

    private void onDelete() {
        int selectedIndex = programListWidget.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < this.menu.getProgram().size()) {
            this.menu.removeOperation(selectedIndex);
            programListWidget.clearSelection();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // AbstractContainerScreen.mouseScrolled 不转发给子 Widget，手动分发
        for (var widget : this.children()) {
            if (widget instanceof AbstractWidget aw
                && mouseX >= aw.getX() && mouseX < aw.getX() + aw.getWidth()
                && mouseY >= aw.getY() && mouseY < aw.getY() + aw.getHeight()) {
                if (aw.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
