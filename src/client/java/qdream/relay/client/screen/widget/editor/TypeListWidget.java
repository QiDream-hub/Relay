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
 * 数据类型列表 Widget
 * 显示可用的 Iota 数据类型（如 string, number, boolean 等）
 * 支持滚动浏览、点击添加、悬停高亮
 * 点击某类型时通过回调通知父级添加该类型
 */
public class TypeListWidget extends AbstractWidget {

    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 3;
    private static final int HEADER_HEIGHT = 16;
    
    private static final int BG_COLOR = 0xFF1E1E1E;
    private static final int HEADER_BG = 0xFF252525;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int TEXT_COLOR = 0xFFAAAAFF;
    private static final int HOVER_COLOR = 0xFFCCCCFF;
    private static final int HOVER_BG = 0x308080FF;
    private static final int SELECTED_BG = 0x408080FF;
    private static final int SCROLLBAR_COLOR = 0xFF505050;
    private static final int SCROLLBAR_HOVER = 0xFF707070;
    private static final int SCROLLBAR_BG = 0xFF2A2A2A;

    private final Font font;
    private final List<String> dataTypes;
    private Consumer<String> onTypeClicked;

    /** 悬停索引 */
    private int hoveredIndex = -1;

    /** 滚动偏移量（以行为单位） */
    private int scrollOffset = 0;

    /** 当前选中的条目索引，-1 表示无选中 */
    private int selectedIndex = -1;

    /** 滚动条悬停状态 */
    private boolean scrollbarHovered = false;

    public TypeListWidget(int x, int y, int width, int height, Font font, List<String> dataTypes) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.dataTypes = dataTypes;
    }

    public void setOnTypeClicked(Consumer<String> callback) {
        this.onTypeClicked = callback;
    }

    /** 获取可视区域内可显示的最大行数 */
    private int getVisibleLineCount() {
        return Math.max(1, (this.height - HEADER_HEIGHT - PADDING * 2) / LINE_HEIGHT);
    }

    /** 获取最大滚动偏移 */
    private int getMaxScroll() {
        return Math.max(0, dataTypes.size() - getVisibleLineCount());
    }

    /** 根据鼠标坐标计算条目索引，超出范围返回 -1 */
    private int getEntryAt(double mouseX, double mouseY) {
        double relX = mouseX - (getX() + PADDING);
        double relY = mouseY - (getY() + HEADER_HEIGHT + PADDING);
        if (relX < 0 || relX > this.width - PADDING * 2 - 4 || relY < 0)
            return -1;
        int index = scrollOffset + (int) (relY / LINE_HEIGHT);
        if (index < 0 || index >= dataTypes.size())
            return -1;
        return index;
    }

    /** 检查鼠标是否在滚动条上 */
    private boolean isMouseOnScrollbar(double mouseX, double mouseY) {
        int sbX = getX() + this.width - 4;
        return mouseX >= sbX && mouseX <= getX() + this.width;
    }

    // ==================== 事件处理 ====================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.visible) return false;
        
        if (event.button() == 0) {
            int index = getEntryAt(event.x(), event.y());
            if (index >= 0 && index < dataTypes.size()) {
                selectedIndex = index;
                if (onTypeClicked != null) {
                    onTypeClicked.accept(dataTypes.get(index));
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dataTypes.size() <= getVisibleLineCount()) return false;
        scrollOffset -= (int) scrollY;
        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()));
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && 
               mouseX >= this.getX() && mouseX < this.getX() + this.width &&
               mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }

    // ==================== 渲染 ====================

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int visibleLines = getVisibleLineCount();

        // 背景
        graphics.fill(x, y, x + this.width, y + this.height, BG_COLOR);
        
        // 外边框
        graphics.outline(x, y, this.width, this.height, BORDER_COLOR);

        // 标题栏背景
        graphics.fill(x, y, x + this.width, y + HEADER_HEIGHT, HEADER_BG);
        graphics.horizontalLine(x, x + this.width, y + HEADER_HEIGHT, BORDER_COLOR);

        // 更新悬停索引
        hoveredIndex = getEntryAt(mouseX, mouseY);
        scrollbarHovered = isMouseOnScrollbar(mouseX, mouseY);

        // 启用裁剪区域（底部留出计数提示的空间）
        int contentBottom = y + this.height - LINE_HEIGHT - 4;
        graphics.enableScissor(x + PADDING, y + HEADER_HEIGHT + PADDING, 
                               x + this.width - PADDING - 4, contentBottom);

        // 渲染列表条目
        int textX = x + PADDING + 2;
        int textY = y + HEADER_HEIGHT + PADDING;

        for (int i = 0; i < visibleLines && (i + scrollOffset) < dataTypes.size(); i++) {
            int dataIndex = i + scrollOffset;
            String typeId = dataTypes.get(dataIndex);
            int entryY = textY + i * LINE_HEIGHT;

            boolean isHovered = (dataIndex == hoveredIndex);
            boolean isSelected = (dataIndex == selectedIndex);

            // 选中背景优先于悬停背景
            if (isSelected) {
                graphics.fill(x + PADDING, entryY - 1, x + this.width - PADDING - 4, entryY + LINE_HEIGHT - 1, SELECTED_BG);
            } else if (isHovered) {
                graphics.fill(x + PADDING, entryY - 1, x + this.width - PADDING - 4, entryY + LINE_HEIGHT - 1, HOVER_BG);
            }

            int color = isHovered ? HOVER_COLOR : TEXT_COLOR;
            graphics.text(this.font, typeId, textX, entryY, color);
        }

        graphics.disableScissor();

        // 渲染滚动条（仅在内容超出时显示）
        if (dataTypes.size() > visibleLines) {
            renderScrollBar(graphics, x, y, visibleLines);
        }

        // 标题文字
        graphics.text(this.font, "数据类型", x + PADDING + 2, y + 4, 0xFFAAAAFF);

        // 底部计数提示（在裁剪区域外渲染，确保可见）
        int countY = y + this.height - LINE_HEIGHT - 2;
        graphics.text(this.font, dataTypes.size() + " 个类型", x + PADDING, countY, 0xFF666666);
    }

    /**
     * 渲染右侧滚动条
     */
    private void renderScrollBar(GuiGraphicsExtractor graphics, int x, int y, int visibleLines) {
        int sbX = x + this.width - 4;
        int sbTop = y + HEADER_HEIGHT + PADDING;
        int sbHeight = this.height - HEADER_HEIGHT - PADDING * 2;

        // 滚动条背景
        graphics.fill(sbX, sbTop, sbX + 4, sbTop + sbHeight, SCROLLBAR_BG);

        // 滚动条滑块
        float ratio = (float) visibleLines / dataTypes.size();
        int thumbHeight = Math.max(10, (int) (sbHeight * ratio));
        float scrollRatio = getMaxScroll() > 0 ? (float) scrollOffset / getMaxScroll() : 0;
        int thumbY = sbTop + (int) ((sbHeight - thumbHeight) * scrollRatio);

        int thumbColor = scrollbarHovered ? SCROLLBAR_HOVER : SCROLLBAR_COLOR;
        graphics.fill(sbX, thumbY, sbX + 4, thumbY + thumbHeight, thumbColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // 无障碍：无需额外描述
    }
}