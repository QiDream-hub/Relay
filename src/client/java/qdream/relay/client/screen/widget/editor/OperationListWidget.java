package qdream.relay.client.screen.widget.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 可用操作列表 Widget
 * 支持滚动浏览、点击选中、悬停高亮
 * 点击某项操作时通过回调通知父级 Screen
 */
public class OperationListWidget extends AbstractWidget {

    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int TEXT_COLOR = 0xFF00AA00;
    private static final int HOVER_COLOR = 0xFF00FF00;
    private static final int HOVER_BG = 0x4000FF00;
    private static final int SCROLLBAR_COLOR = 0xFF808080;
    private static final int SCROLLBAR_BG = 0xFF303030;

    private final Font font;
    private final List<String> operations;
    private Consumer<String> onOperationClicked;

    /** 滚动偏移量（以行为单位） */
    private int scrollOffset = 0;

    /** 当前鼠标悬停的条目索引，-1 表示无悬停 */
    private int hoveredIndex = -1;

    public OperationListWidget(int x, int y, int width, int height, Font font, List<String> operations) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.operations = operations;
    }

    public void setOnOperationClicked(Consumer<String> callback) {
        this.onOperationClicked = callback;
    }

    /** 获取可视区域内可显示的最大行数 */
    private int getVisibleLineCount() {
        return Math.max(1, (this.height - PADDING * 2) / LINE_HEIGHT);
    }

    /** 获取最大滚动偏移 */
    private int getMaxScroll() {
        return Math.max(0, operations.size() - getVisibleLineCount());
    }

    /** 根据鼠标坐标计算条目索引，超出范围返回 -1 */
    private int getEntryAt(double mouseX, double mouseY) {
        double relX = mouseX - (getX() + PADDING);
        double relY = mouseY - (getY() + PADDING);
        if (relX < 0 || relX > this.width - PADDING * 2 || relY < 0) return -1;
        int index = scrollOffset + (int) (relY / LINE_HEIGHT);
        if (index < 0 || index >= operations.size()) return -1;
        return index;
    }

    // ==================== 事件处理 ====================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int index = getEntryAt(event.x(), event.y());
            if (index >= 0 && index < operations.size()) {
                if (onOperationClicked != null) {
                    onOperationClicked.accept(operations.get(index));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int) scrollY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()));
        return true;
    }

    // ==================== 渲染 ====================

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int visibleLines = getVisibleLineCount();

        // 背景
        graphics.fill(x, y, x + this.width, y + this.height, 0xFF1A1A1A);

        // 启用裁剪区域
        graphics.enableScissor(x + PADDING, y + PADDING, x + this.width - PADDING, y + this.height - PADDING);

        // 更新悬停索引
        hoveredIndex = getEntryAt(mouseX, mouseY);

        // 渲染列表条目
        int textX = x + PADDING + 2;
        int textY = y + PADDING;
        for (int i = 0; i < visibleLines && (i + scrollOffset) < operations.size(); i++) {
            int dataIndex = i + scrollOffset;
            String op = operations.get(dataIndex);
            int entryY = textY + i * LINE_HEIGHT;

            boolean hovered = (dataIndex == hoveredIndex);

            // 悬停背景
            if (hovered) {
                graphics.fill(x + PADDING, entryY - 1, x + this.width - PADDING, entryY + LINE_HEIGHT - 1, HOVER_BG);
            }

            int color = hovered ? HOVER_COLOR : TEXT_COLOR;
            graphics.text(this.font, op, textX, entryY, color);
        }

        graphics.disableScissor();

        // 渲染滚动条（仅在内容超出时显示）
        if (operations.size() > visibleLines) {
            renderScrollBar(graphics, x, y, visibleLines);
        }

        // 底部计数提示
        int countY = y + this.height - LINE_HEIGHT;
        graphics.text(this.font, "共 " + operations.size() + " 个操作", x + PADDING, countY, 0xFF666666);
    }

    /**
     * 渲染右侧滚动条
     */
    private void renderScrollBar(GuiGraphicsExtractor graphics, int x, int y, int visibleLines) {
        int sbX = x + this.width - 4;
        int sbTop = y + PADDING;
        int sbHeight = this.height - PADDING * 2;

        // 滚动条背景
        graphics.fill(sbX, sbTop, sbX + 3, sbTop + sbHeight, SCROLLBAR_BG);

        // 滚动条滑块
        float ratio = (float) visibleLines / operations.size();
        int thumbHeight = Math.max(8, (int) (sbHeight * ratio));
        float scrollRatio = getMaxScroll() > 0 ? (float) scrollOffset / getMaxScroll() : 0;
        int thumbY = sbTop + (int) ((sbHeight - thumbHeight) * scrollRatio);

        graphics.fill(sbX, thumbY, sbX + 3, thumbY + thumbHeight, SCROLLBAR_COLOR);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // 无障碍：无需额外描述
    }
}
