package qdream.relay.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import qdream.relay.items.ToolShellScreenHandler;

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

    public ToolShellScreen(ToolShellScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // 配置按钮：是否使用背包内能量模块
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonX = this.leftPos + GUI_WIDTH - buttonWidth - 8;
        int buttonY = this.topPos + 8;

        useInventoryEnergyButton = Button.builder(
            getButtonText(),
            btn -> toggleUseInventoryEnergy()
        )
        .bounds(buttonX, buttonY, buttonWidth, buttonHeight)
        .build();

        this.addRenderableWidget(useInventoryEnergyButton);
    }

    /**
     * 获取按钮文本
     */
    private Component getButtonText() {
        boolean useInventory = this.menu.isUseInventoryEnergyModule();
        return Component.literal(useInventory ? "§a 使用背包能量" : "§7 使用插槽能量");
    }

    /**
     * 切换配置
     */
    private void toggleUseInventoryEnergy() {
        boolean newValue = !this.menu.isUseInventoryEnergyModule();
        this.menu.setUseInventoryEnergyModule(newValue);
        useInventoryEnergyButton.setMessage(getButtonText());
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
    public boolean isPauseScreen() {
        return false;
    }
}
