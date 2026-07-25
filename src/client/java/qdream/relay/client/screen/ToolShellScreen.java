package qdream.relay.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import qdream.relay.networking.payloads.C2S_ToolShellConfigPayload;
import qdream.relay.screen.ToolShellScreenHandler;

/**
 * 工具外壳屏幕
 * 显示 4 个插槽：核心、法术磁盘、能量模块、世界交互器
 * 带配置按钮：是否使用背包内能量模块
 */
public class ToolShellScreen extends AbstractContainerScreen<ToolShellScreenHandler> {

    // 布局常量
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 222;
    private static final int LABEL_X = 12;
    private static final int LABEL_START_Y = 14;
    private static final int LABEL_SPACING_Y = 30;
    private static final int BG_COLOR = 0xFF1A1A2E;
    private static final int BORDER_COLOR = 0xFF404060;

    // 插槽标签
    private static final String[] SLOT_LABELS = { "核心", "磁盘", "能量", "交互" };
    private static final int[] SLOT_LABEL_COLORS = { 0xFF00FF88, 0xFF00CCFF, 0xFFFFCC00, 0xFFFF8800 };

    // 配置按钮
    private Button useInventoryEnergyButton;
    private Button debugOutputButton;
    private Button statusInfoButton;

    public ToolShellScreen(ToolShellScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // 配置按钮：是否使用背包内能量模块
        int buttonWidth = 70;
        int buttonHeight = 20;
        int buttonX = this.leftPos + GUI_WIDTH - buttonWidth - 8;
        int buttonY = this.topPos + 16;

        // 初始化时使用默认文本，extractRenderState 会每帧同步实际状态
        useInventoryEnergyButton = Button.builder(
                getUseInventoryEnergyLabel(),
                btn -> toggleUseInventoryEnergy())
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();

        this.addRenderableWidget(useInventoryEnergyButton);

        // 配置按钮:是否启用调试输出
        buttonY += buttonHeight + 8;
        debugOutputButton = Button.builder(
                getDebugOutputLabel(),
                btn -> toggleDebugOutput())
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();
        this.addRenderableWidget(debugOutputButton);

        // 配置按钮:是否启用统计信息
        buttonY += buttonHeight + 8;
        statusInfoButton = Button.builder(
                getStatusLabel(),
                btn -> toggleStatusInfo())
                .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
                .build();
        this.addRenderableWidget(statusInfoButton);
    }

    private Component getUseInventoryEnergyLabel() {
        boolean useInventory = this.menu.isUseInventoryEnergyModule();
        return Component.literal(useInventory ? "§a 使用背包能量" : "§7 使用插槽能量");
    }

    private Component getDebugOutputLabel() {
        boolean debugEnabled = this.menu.isDebugOutputEnabled();
        return Component.literal(debugEnabled ? "§c 调试输出:开" : "§7 调试输出:关");
    }

    private Component getStatusLabel() {
        boolean debugEnabled = this.menu.isStatusInfo();
        return Component.literal(debugEnabled ? "§c 统计信息:开" : "§7 统计信息:关");
    }

    /**
     * 切换配置 - 使用统一网络包发送所有配置项
     */
    private void toggleUseInventoryEnergy() {
        boolean newValue = !this.menu.isUseInventoryEnergyModule();
        sendConfigUpdate(newValue, this.menu.isDebugOutputEnabled(), this.menu.isStatusInfo());
        // 立即更新本地 UI（不等待服务端同步）
        this.menu.setUseInventoryEnergyModule(newValue);
    }

    private void toggleDebugOutput() {
        boolean newValue = !this.menu.isDebugOutputEnabled();
        sendConfigUpdate(this.menu.isUseInventoryEnergyModule(), newValue, this.menu.isStatusInfo());
        // 立即更新本地 UI（不等待服务端同步）
        this.menu.setDebugOutputEnabled(newValue);
    }

    private void toggleStatusInfo() {
        boolean newValue = !this.menu.isStatusInfo();
        sendConfigUpdate(this.menu.isUseInventoryEnergyModule(), this.menu.isDebugOutputEnabled(), newValue);
        // 立即更新本地 UI（不等待服务端同步）
        this.menu.setStatusInfo(newValue);
    }

    /**
     * 发送统一配置更新网络包
     */
    private void sendConfigUpdate(boolean useInventoryEnergy, boolean debugOutput, boolean statusInfo) {
        ClientPlayNetworking.send(new C2S_ToolShellConfigPayload(useInventoryEnergy, debugOutput, statusInfo));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;

        // 背景
        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, BG_COLOR);
        graphics.outline(left, top, this.imageWidth, this.imageHeight, BORDER_COLOR);

        // 插槽标签
        for (int i = 0; i < 4; i++) {
            int labelY = top + LABEL_START_Y + i * LABEL_SPACING_Y + 4;
            graphics.text(this.font, SLOT_LABELS[i], left + LABEL_X, labelY, SLOT_LABEL_COLORS[i]);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 每帧更新按钮文本，确保与服务端同步
        if (useInventoryEnergyButton != null) {
            useInventoryEnergyButton.setMessage(getUseInventoryEnergyLabel());
        }
        if (debugOutputButton != null) {
            debugOutputButton.setMessage(getDebugOutputLabel());
        }
        if (statusInfoButton != null) {
            statusInfoButton.setMessage(getStatusLabel());
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
