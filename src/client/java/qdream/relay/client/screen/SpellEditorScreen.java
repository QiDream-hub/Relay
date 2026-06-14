package qdream.relay.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
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
import qdream.relay.client.screen.widget.editor.OperationListWidget;
import qdream.relay.client.screen.widget.editor.ProgramListWidget;
import qdream.relay.client.screen.widget.editor.SignatureInputWidget;
import qdream.relay.client.screen.widget.editor.TypeListWidget;
import qdream.relay.engine.Executable;
import qdream.relay.items.SpellDiskItem;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;
import qdream.relay.mc.base.Operation;
import qdream.relay.networking.payloads.C2S_ProgramModifiedPayload;
import qdream.relay.networking.payloads.C2S_SaveSpellDiskPayload;
import qdream.relay.screen.SpellEditorScreenHandler;

/**
 * 法术编辑器 Screen
 */
public class SpellEditorScreen extends AbstractContainerScreen<SpellEditorScreenHandler> {

    // 客户端程序缓存（从服务端同步）
    private List<Executable> clientProgramCache = new ArrayList<>();

    // 布局常量
    private static final int OPERATIONS_PANEL_WIDTH = 120;
    private static final int PROGRAM_PANEL_WIDTH = 130;
    private static final int PANEL_PADDING = 10;
    private static final int LINE_HEIGHT = 12;
    private static final int GUI_WIDTH = 410;
    private static final int GUI_HEIGHT = 420;
    private static final int EDITOR_PANEL_HEIGHT = 290;
    private static final int LIST_TOP_MARGIN = LINE_HEIGHT + 8;
    private static final int LIST_BOTTOM_MARGIN = 4;
    private static final int OPS_LIST_HEIGHT = 130;
    private static final int TYPE_LIST_HEIGHT = 90;
    private static final int TYPE_LIST_GAP = LINE_HEIGHT + 10;

    // 布局位置变量
    private int rightX;
    private int rightTop;
    private int signatureInputX;
    private int signatureInputY;
    private int signatureInputWidth;

    // 自定义 Widget
    private OperationListWidget operationListWidget;
    private ProgramListWidget programListWidget;
    private TypeListWidget typeListWidget;
    private SignatureInputWidget signatureInputWidget;
    private Button addButton;
    private Button saveButton;

    // 当前选中类型
    private String selectedTypeId = null;

    public SpellEditorScreen(SpellEditorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        // 设置背包标签位置（由 AbstractContainerScreen 自动渲染）
        this.inventoryLabelY = EDITOR_PANEL_HEIGHT + 10;
    }

    @Override
    protected void init() {
        super.init();

        int left = this.leftPos;
        int top = this.topPos;
        int listTop = top + LIST_TOP_MARGIN;

        // 计算右侧面板位置
        rightX = left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING;
        rightTop = top + 50;
        signatureInputWidth = GUI_WIDTH - (OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH + PANEL_PADDING * 4);
        signatureInputX = rightX;
        signatureInputY = rightTop + 20;

        // ===== 左侧面板：操作列表 =====
        operationListWidget = new OperationListWidget(
            left + PANEL_PADDING, listTop,
            OPERATIONS_PANEL_WIDTH - PANEL_PADDING * 2, OPS_LIST_HEIGHT,
            this.font, this.menu.getAvailableOperations()
        );
        operationListWidget.setOnOperationClicked(this::addOperationToClientProgram);
        this.addRenderableWidget(operationListWidget);

        // ===== 左侧面板：类型列表 =====
        int typeListTop = listTop + OPS_LIST_HEIGHT + TYPE_LIST_GAP;
        typeListWidget = new TypeListWidget(
            left + PANEL_PADDING, typeListTop,
            OPERATIONS_PANEL_WIDTH - PANEL_PADDING * 2, TYPE_LIST_HEIGHT,
            this.font, this.menu.getAvailableDataTypes()
        );
        typeListWidget.setOnTypeSelected(this::onTypeSelected);
        this.addRenderableWidget(typeListWidget);

        // ===== 中间面板：程序列表 =====
        int programHeight = EDITOR_PANEL_HEIGHT - LIST_TOP_MARGIN - LIST_BOTTOM_MARGIN;
        programListWidget = new ProgramListWidget(
            left + OPERATIONS_PANEL_WIDTH + PANEL_PADDING, listTop,
            PROGRAM_PANEL_WIDTH - PANEL_PADDING, programHeight,
            this.font
        );
        this.addRenderableWidget(programListWidget);

        // ===== 右侧面板：输入框 Widget =====
        // 先创建一个空的 SignatureInputWidget（无签名时不可见）
        signatureInputWidget = new SignatureInputWidget(
            signatureInputX, signatureInputY,
            signatureInputWidth,
            null,  // 初始没有签名
            this.font
        );
        signatureInputWidget.visible = false;
        this.addRenderableWidget(signatureInputWidget);

        // 添加常量按钮
        addButton = Button.builder(Component.literal("添加"), btn -> onAddConstant())
            .pos(signatureInputX, rightTop + signatureInputWidth + 10)
            .size(40, 20)
            .build();
        addButton.visible = false;
        this.addRenderableWidget(addButton);

        // 保存按钮
        int buttonY = GUI_HEIGHT - 120;
        int buttonW = 60;
        int buttonSpacing = 8;

        saveButton = Button.builder(Component.literal("保存"), btn -> onSave())
            .pos(rightX + 16, top + 16)
            .size(buttonW, 20)
            .build();
        this.addRenderableWidget(saveButton);

        // 加载按钮（保存按钮右边）
        Button loadButton = Button.builder(Component.literal("加载"), btn -> onLoad())
            .pos(rightX + 16 + buttonW + buttonSpacing, top + 16)
            .size(buttonW, 20)
            .build();
        this.addRenderableWidget(loadButton);

        // 清空程序按钮
        this.addRenderableWidget(Button.builder(Component.literal("清空"), btn -> onClear())
            .pos(rightX, buttonY)
            .size(buttonW, 20)
            .build());

        // 删除选中按钮
        this.addRenderableWidget(Button.builder(Component.literal("删除"), btn -> onDelete())
            .pos(rightX + buttonW + buttonSpacing, buttonY)
            .size(buttonW, 20)
            .build());
    }

    // ==================== 事件处理 ====================

    private void onTypeSelected(String typeId) {
        this.selectedTypeId = typeId;

        if (typeId != null) {
            // 获取类型的操作签名
            OperationSignature signature = this.menu.getOperationSignature(typeId);

            if (signature != null && signature.inputCount() > 0) {
                // 有参数：更新并显示 SignatureInputWidget
                signatureInputWidget.updateSignature(signature);
                signatureInputWidget.updatePosition(signatureInputX, signatureInputY);
                signatureInputWidget.visible = true;
                addButton.visible = true;

                // 可选：聚焦第一个输入框
                signatureInputWidget.focusFirst();
            } else {
                // 无参数类型：隐藏输入框，直接添加
                signatureInputWidget.visible = false;
                addButton.visible = false;

                // 无参数数据类型可以直接添加
                JsonObject json = new JsonObject();
                json.addProperty("value", "");
                addDataToClientProgram(typeId, json);
            }
        } else {
            // 未选中任何类型
            signatureInputWidget.visible = false;
            addButton.visible = false;
        }
    }

    private void onAddConstant() {
        if (selectedTypeId == null) return;

        // 从 SignatureInputWidget 获取输入值
        List<String> inputValues = signatureInputWidget.getInputValues();

        if (inputValues.isEmpty()) {
            // 没有输入框，直接添加空值
            JsonObject json = new JsonObject();
            json.addProperty("value", "");
            addDataToClientProgram(selectedTypeId, json);
            return;
        }

        // 检查是否所有输入框都非空
        boolean hasEmpty = inputValues.stream().allMatch(String::isEmpty);
        if (hasEmpty) return;

        // 构建参数 JSON
        JsonObject json = new JsonObject();
        if (inputValues.size() == 1) {
            // 单参数
            json.addProperty("value", inputValues.get(0));
        } else {
            // 多参数
            JsonObject params = new JsonObject();
            for (int i = 0; i < inputValues.size(); i++) {
                params.addProperty("param" + i, inputValues.get(i));
            }
            json.add("params", params);
        }

        addDataToClientProgram(selectedTypeId, json);

        // 清空输入框
        signatureInputWidget.clear();
    }

    /**
     * 添加数据到客户端程序列表并同步到服务端
     */
    private void addDataToClientProgram(String typeId, JsonObject extraFields) {
        if (OperationRegistry.contains(typeId)) {
            JsonObject json = new JsonObject();
            json.addProperty("id", typeId);
            if (extraFields != null) {
                for (var entry : extraFields.entrySet()) {
                    json.add(entry.getKey(), entry.getValue());
                }
            }
            try {
                Executable entry = ((Operation) OperationRegistry.getEntry(typeId).orElse(null).create()).fromJson(json);
                this.clientProgramCache.add(entry);
                this.programListWidget.setProgram(this.clientProgramCache);
                syncProgramToServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加操作到客户端程序列表并同步到服务端
     */
    private void addOperationToClientProgram(String opId) {
        if (OperationRegistry.contains(opId)) {
            JsonObject json = new JsonObject();
            json.addProperty("id", opId);
            try {
                Executable entry = ((Operation) OperationRegistry.getEntry(opId).orElse(null).create()).fromJson(json);
                this.clientProgramCache.add(entry);
                this.programListWidget.setProgram(this.clientProgramCache);
                syncProgramToServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void onSave() {
        // 保存按钮：通知服务端将实体中的程序保存到磁盘
        // 客户端程序列表已经通过 syncProgramToServer 同步到服务端 blockEntity.program
        ClientPlayNetworking.send(new C2S_SaveSpellDiskPayload());
        saveButton.setMessage(Component.literal("已保存"));
    }

    private void onLoad() {
        // 从磁盘物品直接读取程序列表
        ItemStack diskStack = this.menu.getDiskItem();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof SpellDiskItem)) {
            return;
        }

        List<Executable> program = SpellDiskItem.getProgram(diskStack);
        this.clientProgramCache.clear();
        this.clientProgramCache.addAll(program);
        programListWidget.setProgram(program);
        // 加载后自动同步到服务端
        syncProgramToServer();
    }

    private void onClear() {
        this.clientProgramCache.clear();
        this.programListWidget.setProgram(this.clientProgramCache);
        syncProgramToServer();
    }

    private void onDelete() {
        int selectedIndex = programListWidget.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < this.clientProgramCache.size()) {
            this.clientProgramCache.remove(selectedIndex);
            this.programListWidget.setProgram(this.clientProgramCache);
            syncProgramToServer();
            programListWidget.clearSelection();
        }
    }

    /**
     * 将客户端程序列表同步到服务端
     */
    private void syncProgramToServer() {
        try {
            ListTag programList = ProgramCompiler.toNbt(this.clientProgramCache);
            CompoundTag programTag = new CompoundTag();
            programTag.put("program", programList);
            ClientPlayNetworking.send(new C2S_ProgramModifiedPayload(programTag));
        } catch (CompilationException e) {
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
        graphics.verticalLine(left + OPERATIONS_PANEL_WIDTH, top, panelBottom, 0xFF404040);
        graphics.verticalLine(left + OPERATIONS_PANEL_WIDTH + PROGRAM_PANEL_WIDTH, top, panelBottom, 0xFF404040);
        graphics.horizontalLine(left, left + this.imageWidth, panelBottom + 4, 0xFF505050);

        // 面板标题
        graphics.text(this.font, "可用操作", left + PANEL_PADDING, top + 5, 0xFF00FF00);
        graphics.text(this.font, "程序列表", left + OPERATIONS_PANEL_WIDTH + PANEL_PADDING, top + 5, 0xFFFFFF00);
        graphics.text(this.font, "法术磁盘", rightX, top + 5, 0xFF00FFFF);

        // 类型列表标题
        int typeListTop = top + LIST_TOP_MARGIN + OPS_LIST_HEIGHT + TYPE_LIST_GAP;
        graphics.text(this.font, "数据类型", left + PANEL_PADDING, typeListTop - LINE_HEIGHT - 2, 0xFFAAAAFF);

        // 输入提示
        if (selectedTypeId != null && signatureInputWidget != null && signatureInputWidget.visible) {
            graphics.text(this.font, "参数输入:", rightX, rightTop, 0xFFCCCCCC);
        }

        // 操作提示
        int hintY = top + EDITOR_PANEL_HEIGHT - 14;
        graphics.text(this.font, "点击添加操作", left + PANEL_PADDING, hintY, 0xFF666666);
        graphics.text(this.font, "点击选中/删除", left + OPERATIONS_PANEL_WIDTH + PANEL_PADDING, hintY, 0xFF666666);
    }

    // ==================== 事件转发（AbstractContainerScreen 会拦截键盘事件） ====================

    @Override
    public boolean keyPressed(KeyEvent event) {
        // 优先转发给 SignatureInputWidget 中的 EditBox
        if (signatureInputWidget != null && signatureInputWidget.visible) {
            if (signatureInputWidget.keyPressed(event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        // 优先转发给 SignatureInputWidget 中的 EditBox
        if (signatureInputWidget != null && signatureInputWidget.visible) {
            if (signatureInputWidget.charTyped(event)) {
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 优先转发给自定义 Widget（避免被 AbstractContainerScreen 的插槽逻辑拦截）
        for (var widget : this.children()) {
            if (widget instanceof AbstractWidget aw
                && aw.visible
                && event.x() >= aw.getX() && event.x() < aw.getX() + aw.getWidth()
                && event.y() >= aw.getY() && event.y() < aw.getY() + aw.getHeight()) {
                if (aw.mouseClicked(event, doubleClick)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // ==================== 滚轮转发 ====================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
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
    
    // ==================== 服务端同步 ====================

    /**
     * 从服务端同步程序列表
     * 由网络包接收器调用
     */
    public void updateProgramFromServer(List<Executable> program) {
        this.clientProgramCache.clear();
        this.clientProgramCache.addAll(program);
        programListWidget.setProgram(program);
    }

    /**
     * Screen 关闭时清理引用
     */
    @Override
    public void onClose() {
        super.onClose();
    }
}