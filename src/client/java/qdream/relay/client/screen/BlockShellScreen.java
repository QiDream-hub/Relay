package qdream.relay.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import qdream.relay.networking.payloads.C2S_ToggleShellPayload;
import qdream.relay.networking.payloads.C2S_ShellConfigPayload;
import qdream.relay.screen.BlockShellScreenHandler;
import qdream.relay.client.screen.widget.LogWidget;
import qdream.relay.client.screen.widget.SlotWidget;
import qdream.relay.Relay;
import qdream.relay.client.networking.ClientLogCacheManager;

/**
 * 外壳方块屏幕
 * 显示 4 个插槽：核心、法术磁盘、能量模块、世界交互器
 * 包含开关按钮和状态信息显示
 */
public class BlockShellScreen extends AbstractContainerScreen<BlockShellScreenHandler> {

    // 布局常量
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 222;
    private static final int LABEL_X = 12;
    private static final int LABEL_START_Y = 14;
    private static final int LABEL_SPACING_Y = 30;
    private static final int STATUS_X = 116;
    private static final int STATUS_Y = 38;
    private static final int BUTTON_WIDTH = 70;
    private static final int BUTTON_HEIGHT = 20;

    // 插槽布局（与 ScreenHandler 一致）
    private static final int SLOT_X = 50;
    private static final int SLOT_Y = 12;
    private static final int SLOT_SIZE = 18;

    // 日志窗口布局
    private static final int LOG_WINDOW_X = 8;
    private static final int LOG_WINDOW_Y = 146;
    private static final int LOG_WINDOW_WIDTH = 260;
    private static final int LOG_WINDOW_HEIGHT = 240;

    // 复位按钮 - 开关按钮左侧 颜色
    private static final int BG_COLOR = 0xFF1A1A2E;
    private static final int BORDER_COLOR = 0xFF404060;
    private static final int STATUS_BG_COLOR = 0xFF0F0F1F;

    // 复位按钮 - 开关按钮左侧 插槽标签
    private static final Component[] SLOT_LABELS = {
            Component.translatable("gui.relay:shell.slot.core"),
            Component.translatable("gui.relay:shell.slot.disk"),
            Component.translatable("gui.relay:shell.slot.energy"),
            Component.translatable("gui.relay:shell.slot.interactor")
    };
    private static final int[] SLOT_LABEL_COLORS = { 0xFF00FF88, 0xFF00CCFF, 0xFFFFCC00, 0xFFFF8800 };

    // 玩家背包插槽位置
    private static final int INVENTORY_START_X = 8;
    private static final int INVENTORY_START_Y = 140;
    private static final int HOTBAR_START_Y = 198;

    // 开关按钮和调试按钮
    private Button toggleButton;
    private Button debugOutputButton;
    private Button statusInfoButton;

    // 日志窗口 Widget（包级可见，供 RelayClientNetworking 访问）
    private LogWidget logWidget;

    // 插槽 Widget
    private SlotWidget[] containerSlotWidgets;
    private SlotWidget[] inventorySlotWidgets;
    private SlotWidget[] hotbarSlotWidgets;

    public BlockShellScreen(BlockShellScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        // 开关按钮
        toggleButton = Button.builder(getToggleLabel(), btn -> onToggle())
                .pos(this.leftPos + 94, this.topPos + 8)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(toggleButton);

        // 调试输出按钮
        debugOutputButton = Button.builder(
                getDebugOutputLabel(),
                btn -> toggleDebugOutput())
                .pos(this.leftPos + 94, this.topPos + 8 + BUTTON_HEIGHT)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(debugOutputButton);

        // 统计信息按钮
        statusInfoButton = Button.builder(
                getStatusInfoLabel(),
                btn -> toggleStatusInfo())
                .pos(this.leftPos + 94, this.topPos + 8 + BUTTON_HEIGHT * 2)
                .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(statusInfoButton);

        // 日志窗口 Widget - 与 GUI 等宽，放在插槽标签下方
        // 使用客户端缓存管理器获取日志
        BlockPos blockPos = this.menu.getBlockPos();
        Relay.LOGGER.info(blockPos.toString());
        logWidget = new LogWidget(
                LOG_WINDOW_X,
                LOG_WINDOW_Y,
                LOG_WINDOW_WIDTH,
                LOG_WINDOW_HEIGHT,
                this.font,
                () -> {
                    Level level = this.minecraft != null && this.minecraft.level != null ? this.minecraft.level : null;
                    return level != null ? ClientLogCacheManager.getLogs(level, blockPos) : java.util.Collections.emptyList();
                });
        this.addRenderableWidget(logWidget);

        // 容器插槽 Widget (4 个垂直排列)
        containerSlotWidgets = new SlotWidget[4];
        for (int i = 0; i < 4; i++) {
            containerSlotWidgets[i] = new SlotWidget(
                    this.leftPos + SLOT_X,
                    this.topPos + SLOT_Y + i * LABEL_SPACING_Y);
            this.addRenderableWidget(containerSlotWidgets[i]);
        }

        // 玩家背包插槽 Widget（主物品栏 3 行 x 9 列）
        inventorySlotWidgets = new SlotWidget[27];
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                inventorySlotWidgets[y * 9 + x] = new SlotWidget(
                        this.leftPos + INVENTORY_START_X + x * SLOT_SIZE,
                        this.topPos + INVENTORY_START_Y + y * SLOT_SIZE);
                this.addRenderableWidget(inventorySlotWidgets[y * 9 + x]);
            }
        }

        // 玩家热键栏插槽 Widget（1 行 x 9 列）
        hotbarSlotWidgets = new SlotWidget[9];
        for (int x = 0; x < 9; x++) {
            hotbarSlotWidgets[x] = new SlotWidget(
                    this.leftPos + INVENTORY_START_X + x * SLOT_SIZE,
                    this.topPos + HOTBAR_START_Y);
            this.addRenderableWidget(hotbarSlotWidgets[x]);
        }
    }

    private Component getToggleLabel() {
        return Component.translatable(
                this.menu.isEnabled() ? "gui.relay:shell.button.toggle" : "gui.relay:shell.button.toggle.disabled");
    }

    private Component getDebugOutputLabel() {
        boolean debugEnabled = this.menu.isDebugOutputEnabled();
        String key = debugEnabled ? "gui.relay:shell.button.debug_output.enabled"
                : "gui.relay:shell.button.debug_output.disabled";
        return Component.translatable(key);
    }

    private Component getStatusInfoLabel() {
        boolean statusEnabled = this.menu.isStatusInfoEnabled();
        String key = statusEnabled ? "gui.relay:shell.button.status_info.enabled"
                : "gui.relay:shell.button.status_info.disabled";
        return Component.translatable(key);
    }

    private void toggleDebugOutput() {
        boolean newValue = !this.menu.isDebugOutputEnabled();
        // 立即更新本地 UI（不等待服务端同步）
        this.menu.setDebugOutputEnabled(newValue);
        // 发送网络包到服务端
        ClientPlayNetworking.send(new C2S_ShellConfigPayload(
                newValue,
                this.menu.isStatusInfoEnabled()));
    }

    private void toggleStatusInfo() {
        boolean newValue = !this.menu.isStatusInfoEnabled();
        // 立即更新本地 UI（不等待服务端同步）
        this.menu.setStatusInfoEnabled(newValue);
        // 发送网络包到服务端
        ClientPlayNetworking.send(new C2S_ShellConfigPayload(
                this.menu.isDebugOutputEnabled(),
                newValue));
    }

    private void onToggle() {
        toggleButton.setMessage(getToggleLabel());

        // 发送网络包让服务端切换实际状态
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
        graphics.fill(left + STATUS_X - 4, top + STATUS_Y - 4 + BUTTON_HEIGHT * 2,
                left + this.imageWidth - 6, top + STATUS_Y + 48 + BUTTON_HEIGHT * 2, STATUS_BG_COLOR);
        graphics.outline(left + STATUS_X - 4, top + STATUS_Y - 4 + BUTTON_HEIGHT * 2,
                this.imageWidth - STATUS_X - 2, 52, BORDER_COLOR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 每帧更新按钮文本，确保与服务端同步
        if (toggleButton != null) {
            toggleButton.setMessage(getToggleLabel());
        }
        if (debugOutputButton != null) {
            debugOutputButton.setMessage(getDebugOutputLabel());
        }
        if (statusInfoButton != null) {
            statusInfoButton.setMessage(getStatusInfoLabel());
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = this.leftPos;
        int top = this.topPos;

        // 插槽标签
        for (int i = 0; i < SLOT_LABELS.length; i++) {
            int labelY = top + LABEL_START_Y + i * LABEL_SPACING_Y + 4;
            graphics.text(this.font, SLOT_LABELS[i], left + LABEL_X, labelY, SLOT_LABEL_COLORS[i]);
        }

        renderStatusInfo(graphics, this.leftPos + STATUS_X, this.topPos + STATUS_Y + BUTTON_HEIGHT * 2);

        // 不再需要定期请求日志同步 - 服务端现在实时推送单条日志
    }

    /**
     * 渲染状态信息
     */
    private void renderStatusInfo(GuiGraphicsExtractor graphics, int x, int y) {
        int lineHeight = 10;
        int currentY = y;

        boolean enabled = this.menu.isEnabled();
        Component statusText = Component
                .translatable(enabled ? "gui.relay:shell.status.running" : "gui.relay:shell.status.stopped");
        graphics.text(this.font, statusText, x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        int coreCount = this.menu.getSyncedCoreCount();
        Component coreText = coreCount > 0
                ? Component.translatable("gui.relay:shell.status.core", coreCount)
                : Component.translatable("gui.relay:shell.status.core.missing");
        graphics.text(this.font, coreText, x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        double energyCost = this.menu.getSyncedEnergyCost();
        Component energyCostText = energyCost > 0
                ? Component.translatable("gui.relay:shell.status.energy_cost", String.format("%.1f", energyCost))
                : Component.translatable("gui.relay:shell.status.energy_cost.none");
        graphics.text(this.font, energyCostText, x, currentY, 0xFFFFFFFF);
        currentY += lineHeight;

        Component runingText = this.menu.isSyncedRuning()
                ? Component.translatable("gui.relay:shell.status.program.loaded")
                : Component.translatable("gui.relay:shell.status.program.missing");
        graphics.text(this.font, runingText, x, currentY, 0xFFFFFFFF);
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
        if (logWidget != null) {
            return logWidget.mouseScrolled(mouseX, mouseY, horizontalAmount,
                    verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
