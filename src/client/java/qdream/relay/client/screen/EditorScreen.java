package qdream.relay.client.screen;

import java.util.List;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import qdream.relay.Relay;
import qdream.relay.client.screen.widget.editor.JsonEditorWidget;
import qdream.relay.client.screen.widget.editor.OperationListWidget;
import qdream.relay.client.screen.widget.editor.TypeListWidget;
import qdream.relay.client.screen.widget.info.HoverInfoWidget;
import qdream.relay.client.screen.widget.info.HoverInfoWidget.InfoContent;
import qdream.relay.client.screen.widget.info.InfoUtils;
import qdream.relay.client.screen.widget.SlotWidget;
import qdream.relay.engine.Executable;
import qdream.relay.items.DiskItem;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;
import qdream.relay.mc.base.Instruction;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.OperationSignature;
import qdream.relay.networking.payloads.C2S_ProgramModifiedPayload;
import qdream.relay.networking.payloads.C2S_SaveSpellDiskPayload;
import qdream.relay.networking.payloads.C2S_DiskInsertedPayload;
import qdream.relay.screen.EditorScreenHandler;

/**
 * 法术编辑器 Screen
 * 使用官方容器纹理渲染玩家背包
 */
public class EditorScreen extends AbstractContainerScreen<EditorScreenHandler> {

    // ===== 布局常量 =====
    private static final int GUI_WIDTH = 410;
    private static final int GUI_HEIGHT = 420;

    // 左侧面板
    private static final int PANEL_WIDTH = 120;
    private static final int PANEL_PADDING = 10;

    // 右侧编辑器区域
    private static final int EDITOR_TOP_MARGIN = 16;
    private static final int EDITOR_BOTTOM_MARGIN = 8;
    private static final int EDITOR_PANEL_HEIGHT = 290;

    // 列表布局
    private static final int LIST_TOP_MARGIN = 8;
    private static final int OPS_LIST_HEIGHT = 160; // 操作列表高度 (增加 30px)
    private static final int TYPE_LIST_HEIGHT = 110; // 类型列表高度 (增加 20px)
    private static final int LIST_GAP = 8; // 两个列表之间的间距

    // 按钮
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 6;

    // 颜色
    private static final int BG_COLOR = 0xFF101010;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int SEPARATOR_COLOR = 0xFF404040;
    private static final int INVENTORY_SEPARATOR_COLOR = 0xFF505050;
    private static final int TITLE_COLOR = 0xFF00FF00;

    // 磁盘插槽位置（与 ScreenHandler 一致）
    private static final int DISK_SLOT_X = 160;
    private static final int DISK_SLOT_Y = 18;

    // 自定义 Widget
    private OperationListWidget operationListWidget;
    private TypeListWidget typeListWidget;
    private JsonEditorWidget jsonEditorWidget;
    private Button saveButton;
    private Button loadButton;
    private Button formatButton;
    private HoverInfoWidget hoverInfoWidget;
    private SlotWidget diskSlotWidget;

    public EditorScreen(EditorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        // 设置背包标签位置
        this.inventoryLabelY = EDITOR_PANEL_HEIGHT + 10;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;

        // ===== 左侧面板布局 =====
        int listTop = top + LIST_TOP_MARGIN;
        int listWidth = PANEL_WIDTH - PANEL_PADDING * 2;

        // ===== 右侧编辑器区域 =====
        int editorX = left + PANEL_WIDTH + PANEL_PADDING;
        int editorY = top + EDITOR_TOP_MARGIN;
        int editorWidth = GUI_WIDTH - PANEL_WIDTH - PANEL_PADDING * 3;
        int editorHeight = EDITOR_PANEL_HEIGHT - EDITOR_TOP_MARGIN - EDITOR_BOTTOM_MARGIN - BUTTON_HEIGHT;

        // 右侧按钮位置
        int buttonY = top + EDITOR_TOP_MARGIN;
        int buttonX = editorX + editorWidth - BUTTON_WIDTH * 3 - BUTTON_SPACING * 2;

        // ===== 创建 Widget =====
        // 操作列表
        operationListWidget = new OperationListWidget(
                left + PANEL_PADDING, listTop,
                listWidth, OPS_LIST_HEIGHT,
                this.font, this.menu.getAvailableOperations());
        operationListWidget.setOnOperationClicked(this::onOperationClicked);
        operationListWidget.setOnHover(this::onOperationHovered);
        this.addRenderableWidget(operationListWidget);

        // 类型列表
        int typeListTop = listTop + OPS_LIST_HEIGHT + LIST_GAP;
        typeListWidget = new TypeListWidget(
                left + PANEL_PADDING, typeListTop,
                listWidth, TYPE_LIST_HEIGHT,
                this.font, this.menu.getAvailableDataTypes());
        typeListWidget.setOnTypeClicked(this::onTypeClicked);
        typeListWidget.setOnHover(this::onTypeHovered);
        this.addRenderableWidget(typeListWidget);

        // ===== 右侧面板：JSON 编辑器 =====
        jsonEditorWidget = new JsonEditorWidget(
                editorX, editorY + BUTTON_HEIGHT + LIST_GAP,
                editorWidth, editorHeight,
                this.font);
        this.addRenderableWidget(jsonEditorWidget);

        // ===== 功能按钮 =====
        // 格式化按钮
        formatButton = Button.builder(Component.translatable("gui.relay:spell_editor.button.format"), btn -> onFormat())
                .pos(buttonX, buttonY)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(formatButton);

        // 加载按钮
        loadButton = Button.builder(Component.translatable("gui.relay:spell_editor.button.load"), btn -> onLoad())
                .pos(buttonX + BUTTON_WIDTH + BUTTON_SPACING, buttonY)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(loadButton);

        // 保存按钮
        saveButton = Button.builder(Component.translatable("gui.relay:spell_editor.button.save"), btn -> onSave())
                .pos(buttonX + (BUTTON_WIDTH + BUTTON_SPACING) * 2, buttonY)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(saveButton);

        // 悬停信息 Widget（初始隐藏在右侧角落）
        hoverInfoWidget = new HoverInfoWidget(
                0 + PANEL_PADDING, editorY + BUTTON_HEIGHT + LIST_GAP,
                150, 160, this.font);
        this.addRenderableWidget(hoverInfoWidget);

        // 磁盘插槽 Widget (与 ScreenHandler 中的 DISK_SLOT 位置一致)
        diskSlotWidget = new SlotWidget(
                left + DISK_SLOT_X, top + DISK_SLOT_Y, Component.translatable("gui.relay:spell_editor.slot.spell_disk"), this.font);
        this.addRenderableWidget(diskSlotWidget);

        // 初始加载程序
        loadProgramFromServer();
    }

    // ==================== 事件处理 ====================

    /**
     * 点击操作列表时，在光标位置插入操作 JSON
     * 插入后自动追加逗号，不选中内容
     */
    private void onOperationClicked(String opId) {
        // 从注册表获取操作实例
        OperationRegistry.get(opId).ifPresent(op -> {
            JsonObject json = new JsonObject();
            ((Operation) op).toJson(json);
            jsonEditorWidget.insertWithComma(json.toString());
        });
    }

    /**
     * 获取操作悬停信息
     */
    private InfoContent getOperationInfo(String opId) {
        return OperationRegistry.get(opId).map(op -> {
            Operation operation = (Operation) op;
            var signature = operation.getSignature();
            InfoContent operationInfo = InfoUtils.buildOperationInfo(opId,
                    signature instanceof OperationSignature opSig ? opSig : null);
            if (operation instanceof Instruction spell) {
                operationInfo.pushLine("计算开销:" + spell.getCost() + " 能量消耗:" + spell.getEnergy(), TITLE_COLOR);
            }
            return operationInfo;
        }).orElse(null);
    }

    /**
     * 获取类型悬停信息
     */
    private InfoContent getTypeInfo(String typeId) {
        return InfoUtils.buildTypeInfo(typeId);
    }

    /**
     * 点击类型列表时，在光标位置插入数据类型 JSON
     * 插入后自动追加逗号，不选中内容
     */
    private void onTypeClicked(String typeId) {
        // 从注册表获取数据类型实例（默认值）
        OperationRegistry.get(typeId).ifPresent(data -> {
            JsonObject json = new JsonObject();
            ((Operation) data).toJson(json);
            jsonEditorWidget.insertWithComma(json.toString());
        });
    }

    /**
     * 操作悬停回调
     */
    private void onOperationHovered(String opId) {
        if (opId != null) {
            InfoContent content = getOperationInfo(opId);
            if (content != null) {
                hoverInfoWidget.setContent(content);
                hoverInfoWidget.visible = true;
            } else {
                hoverInfoWidget.visible = false;
            }
        } else {
            hoverInfoWidget.visible = false;
        }
    }

    /**
     * 类型悬停回调
     */
    private void onTypeHovered(String typeId) {
        if (typeId != null) {
            InfoContent content = getTypeInfo(typeId);
            if (content != null) {
                hoverInfoWidget.setContent(content);
                hoverInfoWidget.visible = true;
            } else {
                hoverInfoWidget.visible = false;
            }
        } else {
            hoverInfoWidget.visible = false;
        }
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

            saveButton.setMessage(Component.translatable("gui.relay:spell_editor.save.success"));
        } catch (CompilationException e) {
            // 保存失败，显示错误
            Relay.LOGGER.error("保存失败 出现错误:" + e.getMessage());
            saveButton.setMessage(Component.translatable("gui.relay:spell_editor.save.failure"));
        }
    }

    /**
     * 从磁盘加载程序
     */
    private void onLoad() {
        ItemStack diskStack = this.menu.getDiskItem();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof DiskItem)) {
            return;
        }

        // 发送请求到服务端，由服务端加载并同步
        ClientPlayNetworking.send(new C2S_DiskInsertedPayload());
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
                this.leftPos + this.imageWidth, this.topPos + this.imageHeight, BG_COLOR);
        graphics.outline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BORDER_COLOR);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;
        int panelBottom = top + EDITOR_PANEL_HEIGHT;

        // 分隔线
        graphics.verticalLine(left + PANEL_WIDTH, top, panelBottom, SEPARATOR_COLOR);
        graphics.horizontalLine(left, left + this.imageWidth, panelBottom + 4, INVENTORY_SEPARATOR_COLOR);

        graphics.text(this.font, Component.translatable("gui.relay:spell_editor.label.editor"), left + PANEL_WIDTH + PANEL_PADDING, top + 5, TITLE_COLOR);

        // 磁盘提示文字
        graphics.text(this.font, Component.translatable("gui.relay:spell_editor.label.disk_slot"), left + DISK_SLOT_X - 25, top + DISK_SLOT_Y, TITLE_COLOR);
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

        // 转发给其他自定义 Widget（按渲染顺序逆序，后渲染的优先接收事件）
        var children = this.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            var widget = children.get(i);
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

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        // 优先转发给 JSON 编辑器
        if (jsonEditorWidget != null && jsonEditorWidget.visible) {
            if (jsonEditorWidget.mouseDragged(event, deltaX, deltaY)) {
                return true;
            }
        }

        // 转发给其他自定义 Widget（按渲染顺序逆序，后渲染的优先接收事件）
        var children = this.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            var widget = children.get(i);
            if (widget instanceof net.minecraft.client.gui.components.AbstractWidget aw
                    && aw.visible
                    && event.x() >= aw.getX() && event.x() < aw.getX() + aw.getWidth()
                    && event.y() >= aw.getY() && event.y() < aw.getY() + aw.getHeight()) {
                if (aw.mouseDragged(event, deltaX, deltaY)) {
                    return true;
                }
            }
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    // ==================== 滚轮转发 ====================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 按渲染顺序的逆序检查 Widget（后渲染的优先接收事件）
        // 这样可以确保类型列表在操作列表下方时，鼠标在类型列表区域时事件正确转发
        var children = this.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            var widget = children.get(i);
            if (widget instanceof net.minecraft.client.gui.components.AbstractWidget aw
                    && aw.visible
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
