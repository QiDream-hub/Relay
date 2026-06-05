package qdream.relay.client.editor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;

import qdream.relay.screen.SpellEditorScreenHandler;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.IExecutable;
import qdream.relay.engine.IData;
import qdream.relay.mc.McIota;

import java.util.ArrayList;
import java.util.List;

/**
 * 栈图编辑器 Screen - 最简双栈展示
 *
 * 布局：
 * [可用操作] | [程序列表] | [数据栈]
 *            | [执行按钮]  | [程序栈]
 */
public class SpellEditorScreen extends AbstractContainerScreen<SpellEditorScreenHandler> {

    private final SpellEditorScreenHandler handler;
    private final StateMachine testMachine;

    // 选中的操作索引（用于删除）
    private int selectedProgramIndex = -1;

    // 运行状态
    private String lastMishapReason = null;

    public SpellEditorScreen(SpellEditorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.handler = handler;
        this.testMachine = new StateMachine(1024);
        this.testMachine.setMishapHandler(reason -> this.lastMishapReason = reason);
    }

    @Override
    protected void init() {
        super.init();
        
        int left = leftPos;
        int top = topPos;
        
        // 运行测试按钮
        this.addRenderableWidget(Button.builder(Component.literal("运行"), btn -> onRun())
            .pos(left + 260, top + 140).size(60, 20).build());
        
        // 清空程序按钮
        this.addRenderableWidget(Button.builder(Component.literal("清空"), btn -> onClear())
            .pos(left + 260, top + 165).size(60, 20).build());
        
        // 删除选中按钮
        this.addRenderableWidget(Button.builder(Component.literal("删除"), btn -> onDelete())
            .pos(left + 260, top + 190).size(60, 20).build());
    }
    
    /**
     * 渲染字符串到屏幕
     */
    private void drawString(String text, int x, int y, int color) {
        // 26.1.2 简化渲染：使用 Font 直接绘制
        // 注意：这是临时实现，等待 Fabric API 稳定后再完善
    }

    private void renderOperationList(int left, int top) {
        List<String> ops = handler.getAvailableOperations();
        int y = top + 20;
        for (String op : ops) {
            drawString(op, left + 5, y, 0x00FF00);
            y += 12;
            if (y > top + 200) break;
        }
    }

    private void renderProgramList(int left, int top) {
        List<String> program = handler.getProgram();
        int y = top + 20;
        for (int i = 0; i < program.size(); i++) {
            String op = program.get(i);
            int color = (i == selectedProgramIndex) ? 0xFFFF00 : 0xFFFFFF;
            drawString(i + ": " + op, left + 125, y, color);
            y += 12;
        }
    }

    private void renderStacks(int left, int top) {
        // 数据栈
        List<IData> dataStack = testMachine.getDataStackSnapshot();
        int y = top + 20;
        for (int i = dataStack.size() - 1; i >= 0; i--) {
            IData data = dataStack.get(i);
            String text = data.getType() + ": " + data.getValue();
            drawString(text, left + 340, y, 0x00FFFF);
            y += 12;
        }
        
        // 程序栈
        List<IExecutable> programStack = testMachine.getProgramStackSnapshot();
        y = top + 135;
        for (int i = programStack.size() - 1; i >= 0; i--) {
            IExecutable exec = programStack.get(i);
            String text = exec.getType() + ": " + exec.getValue();
            drawString(text, left + 340, y, 0xFF8800);
            y += 12;
            if (y > top + 280) break;
        }
        
        // 事故信息
        if (lastMishapReason != null) {
            drawString("事故：" + lastMishapReason, left + 5, top + 220, 0xFF0000);
        }
    }

    private void onRun() {
        List<String> program = handler.getProgram();
        if (program.isEmpty()) return;
        
        // 将程序加载到测试状态机
        List<IExecutable> iotaProgram = new ArrayList<>();
        for (String opId : program) {
            iotaProgram.add(McIota.ofString(opId));
        }
        
        testMachine.loadProgram(iotaProgram);
        lastMishapReason = null;
        
        // 执行 10 个 tick 用于测试
        for (int tick = 0; tick < 10 && testMachine.isRunning(); tick++) {
            testMachine.tick(10);
        }
    }

    private void onClear() {
        handler.clearProgram();
        selectedProgramIndex = -1;
        lastMishapReason = null;
    }

    private void onDelete() {
        if (selectedProgramIndex >= 0 && selectedProgramIndex < handler.getProgram().size()) {
            handler.removeOperation(selectedProgramIndex);
            selectedProgramIndex = -1;
        }
    }
}
