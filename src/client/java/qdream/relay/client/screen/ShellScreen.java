package qdream.relay.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import qdream.relay.networking.payloads.C2S_ToggleShellPayload;
import qdream.relay.screen.ShellScreenHandler;

/**
 * 外壳方块屏幕
 * 显示 4 个插槽：核心、法术磁盘、能量模块、世界交互器
 * 包含开关按钮和状态信息显示
 */
public class ShellScreen extends AbstractContainerScreen<ShellScreenHandler> {

    // 布局常量
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 222;
    private static final int LABEL_X = 12;
    private static final int LABEL_START_Y = 14;
    private static final int LABEL_SPACING_Y = 30;
    private static final int STATUS_X = 116; // 右侧，按钮下方
    private static final int STATUS_Y = 38;  // 按钮下方开始
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 20;

    // 颜色
    private static final int BG_COLOR = 0xFF1A1A2E;
    private static final int BORDER_COLOR = 0xFF404060;
    private static final int STATUS_BG_COLOR = 0xFF0F0F1F;

    // 插槽标签
    private static final String[] SLOT_LABELS = {"核心", "磁盘", "能量", "交互"};
    private static final int[] SLOT_LABEL_COLORS = {0xFF00FF88, 0xFF00CCFF, 0xFFFFCC00, 0xFFFF8800};

    // 开关按钮
    private Button toggleButton;

    public ShellScreen(ShellScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // 开关按钮 - 右上角
        toggleButton = Button.builder(getToggleLabel(), btn -> onToggle())
            .pos(this.leftPos + 116, this.topPos + 8)
            .size(BUTTON_WIDTH, BUTTON_HEIGHT)
            .build();
        this.addRenderableWidget(toggleButton);
    }

    private Component getToggleLabel() {
        return Component.literal(this.menu.isEnabled() ? "§a已开启" : "§c已关闭");
    }

    private void onToggle() {
        // 本地预览切换
        this.menu.toggleEnabled();
        toggleButton.setMessage(getToggleLabel());

        // 发送网络包到服务端切换状态
        ClientPlayNetworking.send(new C2S_ToggleShellPayload());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;

        // 深色背景
        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, BG_COLOR);
        graphics.outline(left, top, this.imageWidth, this.imageHeight, BORDER_COLOR);

        // 状态信息区域背景（右侧按钮下方）
        graphics.fill(left + STATUS_X - 4, top + STATUS_Y - 4,
            left + this.imageWidth - 6, top + STATUS_Y + 48, STATUS_BG_COLOR);
        graphics.outline(left + STATUS_X - 4, top + STATUS_Y - 4,
            this.imageWidth - STATUS_X - 2, 52, BORDER_COLOR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 每帧同步按钮文字
        if (toggleButton != null) {
            toggleButton.setMessage(getToggleLabel());
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;

        // 渲染插槽标签（在插槽左侧）
        for (int i = 0; i < SLOT_LABELS.length; i++) {
            int labelY = top + LABEL_START_Y + i * LABEL_SPACING_Y + 4;
            graphics.text(this.font, SLOT_LABELS[i], left + LABEL_X, labelY, SLOT_LABEL_COLORS[i]);
        }

        // 渲染状态信息区域（右侧按钮下方）
        renderStatusInfo(graphics, left + STATUS_X, top + STATUS_Y);
    }

    /**
     * 渲染状态信息
     */
    private void renderStatusInfo(GuiGraphicsExtractor graphics, int x, int y) {
        int lineHeight = 10;
        int currentY = y;

        // 状态：启用/关闭
        boolean enabled = this.menu.isEnabled();
        String statusText = enabled ? "§a● 运行中" : "§c● 已停止";
        graphics.text(this.font, statusText, x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        // 核心数量
        int coreCount = this.menu.getSyncedCoreCount();
        String coreText = coreCount > 0 ? "§7核心: §f" + coreCount : "§7核心: §8未安装";
        graphics.text(this.font, coreText, x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        // 能量
        int energy = this.menu.getSyncedEnergy();
        String energyText = energy > 0 ? "§7能量: §e" + energy : "§7能量: §8无";
        graphics.text(this.font, energyText, x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        // 初始化状态
        boolean initialized = this.menu.isSyncedInitialized();
        String initText = initialized ? "§7程序: §a已加载" : "§7程序: §8未加载";
        graphics.text(this.font, initText, x, currentY, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
