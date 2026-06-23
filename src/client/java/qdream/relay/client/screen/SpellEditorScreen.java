package qdream.relay.client.screen;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import qdream.relay.client.screen.widget.editor.JsonEditorWidget;
import qdream.relay.client.screen.widget.editor.OperationListWidget;
import qdream.relay.client.screen.widget.editor.TypeListWidget;
import qdream.relay.engine.Executable;
import qdream.relay.items.SpellDiskItem;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;
import qdream.relay.networking.payloads.C2S_ProgramModifiedPayload;
import qdream.relay.networking.payloads.C2S_SaveSpellDiskPayload;
import qdream.relay.screen.SpellEditorScreenHandler;

/**
 * 法术编辑器 Screen
 * 使用官方容器纹理渲染玩家背包
 */
public class SpellEditorScreen extends AbstractContainerScreen<SpellEditorScreenHandler> {

    // 布局常量 - 大尺寸编辑器布局
    private static final int GUI_WIDTH = 410;
    private static final int GUI_HEIGHT = 420;
    private static final int PANEL_WIDTH = 120;
    private static final int PANEL_PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int EDITOR_PANEL_HEIGHT = 290;
    private static final int LIST_TOP_MARGIN = LINE_HEIGHT + 8;
    private static final int LIST_BOTTOM_MARGIN = 4;
    private static final int OPS_LIST_HEIGHT = 130;
    private static final int TYPE_LIST_HEIGHT = 90;
    private static final int TYPE_LIST_GAP = LINE_HEIGHT + 10;
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 20;

    // 自定义 Widget
    private OperationListWidget operationListWidget;
    private TypeListWidget typeListWidget;
    private JsonEditorWidget jsonEditorWidget;
    private Button saveButton;
    private Button loadButton;
    private Button formatButton;

    public SpellEditorScreen(SpellEditorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        // 设置背包标签位置
        this.inventoryLabelY = EDITOR_PANEL_HEIGHT + 10;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;
        int listTop = top + LIST_TOP_MARGIN;

        // 计算右侧编辑器区域
        int editorX = left + PANEL_WIDTH + PANEL_PADDING;
        int editorY = top + LIST_TOP_MARGIN;
        int editorWidth = GUI_WIDTH - PANEL_WIDTH - PANEL_PADDING * 3;
        int editorHeight = EDITOR_PANEL_HEIGHT - LIST_TOP_MARGIN - LIST_BOTTOM_MARGIN - BUTTON_HEIGHT;

        // 计算右侧按钮
        int buttonY = top + 16;
        int buttonSpacing = 6;
        int buttonX = editorX + editorWidth - BUTTON_WIDTH * 3 - buttonSpacing * 2;

        // ===== 左侧面板：操作列表 =====
        operationListWidget = new OperationListWidget(
                left + PANEL_PADDING, listTop,
                PANEL_WIDTH - PANEL_PADDING * 2, OPS_LIST_HEIGHT,
                this.font, this.menu.getAvailableOperations());
        operationListWidget.setOnOperationClicked(this::onOperationClicked);
        this.addRenderableWidget(operationListWidget);

        // ===== 左侧面板：类型列表 =====
        int typeListTop = listTop + OPS_LIST_HEIGHT + TYPE_LIST_GAP;
        typeListWidget = new TypeListWidget(
                left + PANEL_PADDING, typeListTop,
                PANEL_WIDTH - PANEL_PADDING * 2, TYPE_LIST_HEIGHT,
                this.font, this.menu.getAvailableDataTypes());
        typeListWidget.setOnTypeClicked(this::onTypeClicked);
        this.addRenderableWidget(typeListWidget);

        // ===== 右侧面板：JSON 编辑器 =====
        jsonEditorWidget = new JsonEditorWidget(
                editorX, editorY + BUTTON_HEIGHT,
                editorWidth, editorHeight - BUTTON_HEIGHT,
                this.font);
        this.addRenderableWidget(jsonEditorWidget);

        // ===== 功能按钮 =====
        // 格式化按钮
        formatButton = Button.builder(Component.literal("格式化"), btn -> onFormat())
                .pos(buttonX, buttonY)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(formatButton);

        // 加载按钮
        loadButton = Button.builder(Component.literal("加载"), btn -> onLoad())
                .pos(buttonX + BUTTON_WIDTH + buttonSpacing, buttonY)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(loadButton);

        // 保存按钮
        saveButton = Button.builder(Component.literal("保存"), btn -> onSave())
                .pos(buttonX + (BUTTON_WIDTH + buttonSpacing) * 2, buttonY)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(saveButton);

        // 初始加载程序
        loadProgramFromServer();
    }

    // ==================== 事件处理 ====================

    /**
     * 点击操作列表时，在光标位置插入操作 JSON
     */
    private void onOperationClicked(String opId) {
        // 从注册表获取操作实例
        OperationRegistry.get(opId).ifPresent(op -> {
            JsonObject json = new JsonObject();
            ((qdream.relay.mc.base.Operation) op).toJson(json);
            jsonEditorWidget.insertAtCursor(json.toString() + "\n");
        });
    }

    /**
     * 点击类型列表时，在光标位置插入数据类型 JSON
     */
    private void onTypeClicked(String typeId) {
        // 从注册表获取数据类型实例（默认值）
        OperationRegistry.get(typeId).ifPresent(data -> {
            JsonObject json = new JsonObject();
            ((qdream.relay.mc.base.Operation) data).toJson(json);
            jsonEditorWidget.insertAtCursor(json.toString() + "\n");
        });
    }

    // ==================== 按键处理 - 阻止按 E 关闭 GUI ====================

    @Override
    public boolean keyPressed(KeyEvent event) {
        // 如果编辑器聚焦，消费掉 E 键防止关闭 GUI
        if (jsonEditorWidget != null && jsonEditorWidget.isFocused()) {
            // E 键的 keyCode 是 69
            if (event.key() == 69) {
                return true; // 消费掉 E 键
            }
        }

        // 优先转发给 JSON 编辑器
        if (jsonEditorWidget != null) {
            if (jsonEditorWidget.keyPressed(event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    /**
     * 格式化 JSON
     */
    private void onFormat() {
        try {
            String jsonStr = jsonEditorWidget.getJsonContent();
            if (jsonStr.trim().isEmpty()) {
                return;
            }

            // 解析并重新格式化
            List<Executable> program = ProgramCompiler.compileFromJson(jsonStr);
            String formatted = ProgramCompiler.toPrettyJson(program);
            jsonEditorWidget.setJsonContent(formatted);
        } catch (CompilationException e) {
            // 格式化失败，保持原样
        }
    }

    /**
     * 保存程序到磁盘
     */
    private void onSave() {
        try {
            // 从 JSON 编辑器获取内容并解析
            String jsonStr = jsonEditorWidget.getJsonContent();
            List<Executable> program = ProgramCompiler.compileFromJson(jsonStr);

            // 先同步到服务端 BlockEntity
            ListTag programList = ProgramCompiler.toNbt(program);
            CompoundTag programTag = new CompoundTag();
            programTag.put("program", programList);
            ClientPlayNetworking.send(new C2S_ProgramModifiedPayload(programTag));

            // 然后保存到磁盘
            ClientPlayNetworking.send(new C2S_SaveSpellDiskPayload());

            saveButton.setMessage(Component.literal("已保存"));
        } catch (CompilationException e) {
            // 保存失败，显示错误
            saveButton.setMessage(Component.literal("保存失败"));
        }
    }

    /**
     * 从磁盘加载程序
     */
    private void onLoad() {
        ItemStack diskStack = this.menu.getDiskItem();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof SpellDiskItem)) {
            return;
        }

        List<Executable> program = SpellDiskItem.getProgram(diskStack);
        try {
            String formatted = ProgramCompiler.toPrettyJson(program);
            jsonEditorWidget.setJsonContent(formatted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从服务端加载程序
     */
    private void loadProgramFromServer() {
        List<Executable> program = this.menu.getProgramEntries();
        try {
            String formatted = ProgramCompiler.toPrettyJson(program);
            jsonEditorWidget.setJsonContent(formatted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== 渲染 ====================

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 深色背景
        graphics.fill(this.leftPos, this.topPos,
                this.leftPos + this.imageWidth, this.topPos + this.imageHeight,
                0xFF101010);
        graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFF404040);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;
        int panelBottom = top + EDITOR_PANEL_HEIGHT;

        // 分隔线
        graphics.verticalLine(left + PANEL_WIDTH, top, panelBottom, 0xFF404040);
        graphics.horizontalLine(left, left + this.imageWidth, panelBottom + 4, 0xFF505050);

        // 面板标题
        graphics.text(this.font, "可用操作", left + PANEL_PADDING, top + 5, 0xFF00FF00);
        graphics.text(this.font, "JSON 编辑器", left + PANEL_WIDTH + PANEL_PADDING, top + 5, 0xFFFFFF00);

        // 类型列表标题
        int typeListTop = top + LIST_TOP_MARGIN + OPS_LIST_HEIGHT + TYPE_LIST_GAP;
        graphics.text(this.font, "数据类型", left + PANEL_PADDING, typeListTop - LINE_HEIGHT - 2, 0xFFAAAAFF);

        // 操作提示
        int hintY = top + EDITOR_PANEL_HEIGHT - 14;
        graphics.text(this.font, "点击添加操作", left + PANEL_PADDING, hintY, 0xFF666666);
    }


    // ==================== 事件转发 ====================

    @Override
    public boolean charTyped(CharacterEvent event) {
        // 优先转发给 JSON 编辑器
        if (jsonEditorWidget != null) {
            if (jsonEditorWidget.charTyped(event)) {
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 优先转发给 JSON 编辑器
        if (jsonEditorWidget != null && jsonEditorWidget.visible) {
            if (jsonEditorWidget.mouseClicked(event, doubleClick)) {
                return true;
            }
        }

        // 转发给其他自定义 Widget
        for (var widget : this.children()) {
            if (widget instanceof net.minecraft.client.gui.components.AbstractWidget aw
                    && aw.visible
                    && event.x() >= aw.getX() && event.x() < aw.getX() + aw.getWidth()
                    && event.y() >= aw.getY() && event.y() < aw.getY() + aw.getHeight()) {
                if (aw.mouseClicked(event, doubleClick)) {
                    return true;
                }
            }
        }

        // 点击空白区域取消编辑器聚焦
        if (jsonEditorWidget != null && jsonEditorWidget.isFocused()) {
            jsonEditorWidget.setFocused(false);
        }

        return super.mouseClicked(event, doubleClick);
    }

    // ==================== 滚轮转发 ====================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (var widget : this.children()) {
            if (widget instanceof net.minecraft.client.gui.components.AbstractWidget aw
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

    // ==================== 服务端同步 ====================

    /**
     * 从服务端同步程序列表
     * 由网络包接收器调用
     */
    public void updateProgramFromServer(List<Executable> program) {
        try {
            String formatted = ProgramCompiler.toPrettyJson(program);
            jsonEditorWidget.setJsonContent(formatted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Screen 关闭时清理引用
     */
    @Override
    public void onClose() {
        super.onClose();
    }
}
