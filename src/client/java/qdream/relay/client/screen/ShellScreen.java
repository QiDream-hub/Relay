package qdream.relay.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import qdream.relay.networking.payloads.C2S_ToggleShellPayload;
import qdream.relay.networking.payloads.C2S_InitializeShellPayload;
import qdream.relay.networking.payloads.C2S_RequestShellLogPayload;
import qdream.relay.screen.ShellScreenHandler;
import qdream.relay.client.screen.widget.LogWidget;

/**
 * 外壳方块屏幕
 * 显示 4 个插槽：核心、法术磁盘、能量模块、世界交互器
 * 包含开关按钮和状态信息显示
 */
public class ShellScreen extends AbstractContainerScreen<ShellScreenHandler> {

    // 复位按钮 - 开关按钮左侧 布局常量
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 222;
    private static final int LABEL_X = 12;
    private static final int LABEL_START_Y = 14;
    private static final int LABEL_SPACING_Y = 30;
    private static final int STATUS_X = 116;
    private static final int STATUS_Y = 38;
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 20;

    // 日志窗口布局 - 放在屏幕左边，插槽标签下方
    private static final int LOG_WINDOW_X = 8;
    private static final int LOG_WINDOW_Y = 130; 
    private static final int LOG_WINDOW_WIDTH = 260;
    private static final int LOG_WINDOW_HEIGHT = 280;

    // 复位按钮 - 开关按钮左侧 颜色
    private static final int BG_COLOR = 0xFF1A1A2E;
    private static final int BORDER_COLOR = 0xFF404060;
    private static final int STATUS_BG_COLOR = 0xFF0F0F1F;

    // 复位按钮 - 开关按钮左侧 插槽标签
    private static final String[] SLOT_LABELS = { "核心", "磁盘", "能量", "交互" };
    private static final int[] SLOT_LABEL_COLORS = { 0xFF00FF88, 0xFF00CCFF, 0xFFFFCC00, 0xFFFF8800 };

    // 复位按钮 - 开关按钮左侧 开关按钮
    private Button toggleButton;
    private Button initButton;

    // 日志窗口 Widget（包级可见，供 RelayClientNetworking 访问）
    private LogWidget logWidget;

    public ShellScreen(ShellScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // 开关按钮
        toggleButton = Button.builder(getToggleLabel(), btn -> onToggle())
                .pos(this.leftPos + 116, this.topPos + 8)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(toggleButton);

        // 复位按钮
        initButton = Button.builder(getResetLabel(), btn -> onReset())
                .pos(this.leftPos + 116, this.topPos + 8 + BUTTON_HEIGHT)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(initButton);

        // 日志窗口 Widget
        logWidget = new LogWidget(
                LOG_WINDOW_X,
                LOG_WINDOW_Y,
                LOG_WINDOW_WIDTH,
                LOG_WINDOW_HEIGHT,
                this.font,
                () -> this.menu.getSyncedLogs());
        this.addRenderableWidget(logWidget);
    }

    private Component getToggleLabel() {
        return Component.literal(this.menu.isEnabled() ? "§a已开启" : "§c已关闭");
    }

    private Component getResetLabel() {
        return Component.literal("§e 重载程序");
    }

    private void onReset() {
        ClientPlayNetworking.send(new C2S_InitializeShellPayload());
    }

    private void onToggle() {
        this.menu.toggleEnabled();
        toggleButton.setMessage(getToggleLabel());

        ClientPlayNetworking.send(new C2S_ToggleShellPayload());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, BG_COLOR);
        graphics.outline(left, top, this.imageWidth, this.imageHeight, BORDER_COLOR);

        // 信息渲染
        graphics.fill(left + STATUS_X - 4, top + STATUS_Y - 4 + BUTTON_HEIGHT,
                left + this.imageWidth - 6, top + STATUS_Y + 48 + BUTTON_HEIGHT, STATUS_BG_COLOR);
        graphics.outline(left + STATUS_X - 4, top + STATUS_Y - 4 + BUTTON_HEIGHT,
                this.imageWidth - STATUS_X - 2, 52, BORDER_COLOR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (toggleButton != null) {
        }
        if (initButton != null) {
            initButton.setMessage(getResetLabel());
            toggleButton.setMessage(getToggleLabel());
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;

        for (int i = 0; i < SLOT_LABELS.length; i++) {
            int labelY = top + LABEL_START_Y + i * LABEL_SPACING_Y + 4;
            graphics.text(this.font, SLOT_LABELS[i], left + LABEL_X, labelY, SLOT_LABEL_COLORS[i]);
        }

        renderStatusInfo(graphics, left + STATUS_X, top + STATUS_Y + BUTTON_HEIGHT);

        // 定期请求日志同步（每 10 tick）
        if (this.minecraft != null && this.minecraft.level != null &&
                this.minecraft.level.getGameTime() % 10 == 0) {
            ClientPlayNetworking.send(new C2S_RequestShellLogPayload());
        }
    }

    /**
     * 渲染状态信息
     */
    private void renderStatusInfo(GuiGraphicsExtractor graphics, int x, int y) {
        int lineHeight = 10;
        int currentY = y;

        boolean enabled = this.menu.isEnabled();
        String statusText = enabled ? "§a● 运行中" : "§c● 已停止";
        graphics.text(this.font, statusText, x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        int coreCount = this.menu.getSyncedCoreCount();

        String coreText;
        if (coreCount > 0) {
            coreText = "§7核心：§f" + coreCount;
        } else {
            coreText = "§7核心：§8未安装";
        }
        graphics.text(this.font, coreText, x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        double energyCost = this.menu.getSyncedEnergyCost();
        String energyCostText = energyCost > 0 ? "§7能耗：§e" + energyCost + " /tick" : "§7 能耗：§8无";
        graphics.text(this.font, energyCostText, x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        boolean initialized = this.menu.isSyncedInitialized();
        String initText = initialized ? "§7程序: §a已加载" : "§7程序: §8未加载";
        graphics.text(this.font, initText, x, currentY, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 获取日志窗口 Widget（供网络包处理器访问）
     */
    public LogWidget getLogWidget() {
        return logWidget;
    }

    /**
     * 处理滚轮事件 - 滚动日志
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 检查鼠标是否在日志窗口上方
        if (logWidget != null && logWidget.isMouseOver(mouseX - this.leftPos, mouseY - this.topPos)) {
            if (logWidget.handleScroll(verticalAmount)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
