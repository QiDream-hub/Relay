package qdream.relay.client.editor;

import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    // 选中的操作索引（用于删除）
    private int selectedProgramIndex = -1;

    // 运行状态
    private String lastMishapReason = null;

    // 布局常量
    private static final int OPERATIONS_PANEL_WIDTH = 120;
    private static final int PROGRAM_PANEL_WIDTH = 120;
    private static final int PANEL_PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 280;

    // 面板区域
    private static final int OPERATIONS_X = 0;
    private static final int PROGRAM_X = OPERATIONS_PANEL_WIDTH;
    private static final int STACK_X = OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH;

    public SpellEditorScreen(SpellEditorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.testMachine = new StateMachine(1024);
        this.testMachine.setMishapHandler(reason -> this.lastMishapReason = reason);
    }

    @Override
    protected void init() {
        super.init();

        // 控制按钮区域（右侧面板上方）
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
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 渲染深色背景
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF101010);

        // 渲染边框
        graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF404040);

        // 调用父类渲染 Slot 等
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;

        // 渲染分隔线
        graphics.verticalLine(left + OPERATIONS_PANEL_WIDTH, top, top + this.imageHeight, 0xFF404040);
        graphics.verticalLine(left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH, top, top + this.imageHeight, 0xFF404040);

        // 渲染面板标题
        graphics.text(this.font, "可用操作", left + PANEL_PADDING, top + 5, 0x00FF00);
        graphics.text(this.font, "程序列表", left + OPERATIONS_PANEL_WIDTH + PANEL_PADDING * 2, top + 5, 0xFFFF00);
        graphics.text(this.font, "栈视图", left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING * 3, top + 5, 0x00FFFF);

        // 渲染各面板内容
        renderOperationList(graphics, left, top);
        renderProgramList(graphics, left, top);
        renderStacks(graphics, left, top);

        // 渲染事故信息
        if (lastMishapReason != null) {
            int infoX = left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING * 3;
            graphics.text(this.font, "事故：" + lastMishapReason, infoX, this.topPos + 240, 0xFF0000);
        }

        // 渲染操作提示
        graphics.text(this.font, "点击操作添加", left + PANEL_PADDING, this.topPos + this.imageHeight - 55, 0x888888);
        graphics.text(this.font, "点击程序选中", left + OPERATIONS_PANEL_WIDTH + PANEL_PADDING * 2, this.topPos + this.imageHeight - 55, 0x888888);
    }

    /**
     * 渲染可用操作列表
     */
    private void renderOperationList(GuiGraphicsExtractor graphics, int left, int top) {
        List<String> ops = this.menu.getAvailableOperations();
        int y = top + LINE_HEIGHT + 5;
        int maxLines = 18;  // 增加最大行数
        for (int i = 0; i < Math.min(ops.size(), maxLines); i++) {
            String op = ops.get(i);
            int color = 0x00AA00;
            graphics.text(this.font, op, left + PANEL_PADDING, y, color);
            y += LINE_HEIGHT;
        }

        // 渲染操作数量提示
        graphics.text(this.font, "共 " + ops.size() + " 个操作", left + PANEL_PADDING, top + this.imageHeight - 70, 0x666666);
    }

    /**
     * 渲染程序列表
     */
    private void renderProgramList(GuiGraphicsExtractor graphics, int left, int top) {
        List<String> program = this.menu.getProgram();
        int x = left + OPERATIONS_PANEL_WIDTH + PANEL_PADDING * 2;
        int y = top + LINE_HEIGHT + 5;
        for (int i = 0; i < program.size(); i++) {
            String op = program.get(i);
            
            // 渲染选中背景
            if (i == selectedProgramIndex) {
                graphics.fill(x - 2, y - 2, x + 100, y + LINE_HEIGHT - 2, 0x40FFFF00);
            }
            
            int color = (i == selectedProgramIndex) ? 0xFFFF00 : 0xFFFFFF;
            graphics.text(this.font, (i + 1) + ". " + op, x, y, color);
            y += LINE_HEIGHT;
        }
    }

    /**
     * 渲染数据栈和程序栈
     */
    private void renderStacks(GuiGraphicsExtractor graphics, int left, int top) {
        int x = left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING * 3;
        int y = top + LINE_HEIGHT + 5;

        // 数据栈标题
        graphics.text(this.font, "数据栈:", x, y, 0x00FFFF);
        y += LINE_HEIGHT;

        // 数据栈内容
        List<Executable> dataStack = testMachine.getDataStackSnapshot();
        for (int i = dataStack.size() - 1; i >= 0 && y < top + 100; i--) {
            Executable data = dataStack.get(i);
            String text = ((Operation)data).getId();
            graphics.text(this.font, text, x, y, 0x00AAAA);
            y += LINE_HEIGHT;
        }

        // 程序栈标题
        y += 10;
        graphics.text(this.font, "程序栈:", x, y, 0xFF8800);
        y += LINE_HEIGHT;

        // 程序栈内容
        List<Executable> programStack = testMachine.getProgramStackSnapshot();
        for (int i = programStack.size() - 1; i >= 0 && y < top + 200; i--) {
            Executable exec = programStack.get(i);
            String text = ((Operation)exec).getId();
            graphics.text(this.font, text, x, y, 0xFF8800);
            y += LINE_HEIGHT;
        }
    }

    private void onRun() {
        List<String> program = this.menu.getProgram();
        if (program.isEmpty()) return;

        // 将程序加载到测试状态机
        List<Executable> iotaProgram = new ArrayList<>();
        for (String opId : program) {
            iotaProgram.add(new Operation(opId, 0) {
                @Override
                public void execute(StateMachine executor) {
                    // 测试用空实现
                }
            });
        }

        testMachine.loadProgram(iotaProgram);
        lastMishapReason = null;

        // 执行 10 个 tick 用于测试
        for (int tick = 0; tick < 10 && testMachine.isRunning(); tick++) {
            testMachine.run(10);
        }
    }

    private void onClear() {
        this.menu.clearProgram();
        selectedProgramIndex = -1;
        lastMishapReason = null;
    }

    private void onDelete() {
        if (selectedProgramIndex >= 0 && selectedProgramIndex < this.menu.getProgram().size()) {
            this.menu.removeOperation(selectedProgramIndex);
            selectedProgramIndex = -1;
        }
    }

    // 鼠标点击处理 - 用于添加/选中操作
    // 注意：26.1.2 中使用 extractRenderState 渲染，鼠标事件需要特殊处理

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
