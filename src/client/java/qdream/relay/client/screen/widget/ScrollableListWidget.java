package qdream.relay.client.screen.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import qdream.relay.client.screen.widget.editor.tools.InfoContent;

import java.util.List;
import java.util.function.Consumer;

/**
 * 可滚动列表基础组件
 * 提供纯列表渲染、滚动条、鼠标交互功能
 * 不渲染背景、边框、标题，由父组件自行控制
 */
public class ScrollableListWidget extends AbstractWidget {

    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 3;
    private static final int SCROLLBAR_WIDTH = 4;

    protected static final int DEFAULT_TEXT_COLOR = 0xFFFFFFFF;
    protected static final int DEFAULT_HOVER_COLOR = 0xFFFFFF55;
    protected static final int DEFAULT_SELECTED_BG = 0x40FFFFFF;
    protected static final int DEFAULT_HOVER_BG = 0x30FFFF00;
    protected static final int SCROLLBAR_COLOR = 0xFF808080;
    protected static final int SCROLLBAR_HOVER = 0xFF909090;
    protected static final int SCROLLBAR_BG = 0x00000000;

    protected final Font font;
    protected final List<InfoContent> items;

    /** 当前显示的列表（用于搜索过滤），默认指向 items */
    private List<InfoContent> displayItems;

    protected int textColor = DEFAULT_TEXT_COLOR;
    protected int hoverColor = DEFAULT_HOVER_COLOR;
    protected int selectedBg = DEFAULT_SELECTED_BG;
    protected int hoverBg = DEFAULT_HOVER_BG;

    /** 滚动偏移量（以行为单位） */
    private int scrollOffset = 0;

    /** 当前鼠标悬停的条目索引，-1 表示无悬停 */
    private int hoveredIndex = -1;

    /** 当前选中的条目索引，-1 表示无选中 */
    private int selectedIndex = -1;

    /** 滚动条悬停状态 */
    private boolean scrollbarHovered = false;

    /** 是否在拖拽滚动条 */
    private boolean draggingScrollbar = false;

    /** 拖动滚动条时的初始鼠标 Y 位置 */
    private double dragStartY = 0;

    /** 拖动滚动条时的初始滚动偏移 */
    private int dragStartScrollOffset = 0;

    /** 悬停回调 */
    private Consumer<InfoContent> onHover;

    /** 点击回调 */
    private Consumer<String> onItemSelected;

    public ScrollableListWidget(int x, int y, int width, int height, Font font, List<InfoContent> items) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.items = items;
        this.displayItems = items;
    }

    /**
     * 设置颜色方案
     */
    public void setColors(int textColor, int hoverColor, int hoverBg, int selectedBg) {
        this.textColor = textColor;
        this.hoverColor = hoverColor;
        this.hoverBg = hoverBg;
        this.selectedBg = selectedBg;
    }

    /**
     * 获取 items 列表（供子类访问）
     */
    protected List<InfoContent> getItems() {
        return items;
    }

    /**
     * 设置显示的列表（用于搜索过滤）
     * @param newItems 新的显示列表
     */
    public void setDisplayItems(List<InfoContent> newItems) {
        this.displayItems = newItems != null ? newItems : this.items;
        this.scrollOffset = 0;
        this.hoveredIndex = -1;
        this.selectedIndex = -1;
    }

    /**
     * 获取当前显示的列表
     */
    protected List<InfoContent> getDisplayItems() {
        return displayItems;
    }

    /**
     * 渲染边框和标题的钩子方法（由子类实现）
     */
    protected void renderFrame(GuiGraphicsExtractor graphics) {
    }

    /**
     * 设置悬停回调
     */
    public void setOnHover(Consumer<InfoContent> callback) {
        this.onHover = callback;
    }

    /**
     * 设置选中回调
     */
    public void setOnItemSelected(Consumer<String> callback) {
        this.onItemSelected = callback;
    }

    /**
     * 获取可视区域内可显示的最大行数
     */
    public int getVisibleLineCount() {
        return Math.max(1, (this.height - PADDING * 2) / LINE_HEIGHT);
    }

    /**
     * 获取最大滚动偏移
     */
    public int getMaxScroll() {
        return Math.max(0, getDisplayItems().size() - getVisibleLineCount());
    }

    /**
     * 检查是否需要显示滚动条
     * @return 当元素数量超过可视区域行数时返回 true
     */
    public boolean needsScrollbar() {
        return getDisplayItems().size() > getVisibleLineCount();
    }

    /**
     * 根据鼠标坐标计算条目索引，超出范围返回 -1
     */
    public int getEntryAt(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY))
            return -1;

        double relX = mouseX - (getX() + PADDING);
        double relY = mouseY - (getY() + PADDING);

        if (relX < 0 || relX > this.width - PADDING * 2 - SCROLLBAR_WIDTH || relY < 0)
            return -1;

        int index = scrollOffset + (int) (relY / LINE_HEIGHT);
        
        // 检查是否超出实际元素范围
        if (index < 0 || index >= getDisplayItems().size())
            return -1;
        
        // 检查是否超出可视区域（防止在空白区域点击时返回有效索引）
        int visibleIndex = (int) (relY / LINE_HEIGHT);
        if (visibleIndex >= getVisibleLineCount())
            return -1;
        
        return index;
    }

    /**
     * 获取当前鼠标悬停的条目索引
     * @return 悬停的条目索引，-1 表示无悬停
     */
    public int getHoveredIndex() {
        return hoveredIndex;
    }

    /**
     * 获取当前选中的条目索引
     * @return 选中的条目索引，-1 表示无选中
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * 获取当前鼠标指定的元素索引（在组件范围内）
     * @param mouseX 鼠标 X 坐标
     * @param mouseY 鼠标 Y 坐标
     * @return 元素索引，-1 表示无有效元素
     */
    public int getIndexOf(double mouseX, double mouseY) {
        return getEntryAt(mouseX, mouseY);
    }

    /**
     * 设置选中的索引
     */
    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
    }

    /**
     * 设置滚动偏移量
     */
    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, getMaxScroll()));
    }

    /**
     * 获取滚动偏移量
     */
    public int getScrollOffset() {
        return scrollOffset;
    }

    /**
     * 检查鼠标是否在滚动条上
     */
    private boolean isMouseOnScrollbar(double mouseX, double mouseY) {
        int sbX = getX() + this.width - SCROLLBAR_WIDTH;
        return mouseX >= sbX && mouseX <= getX() + this.width;
    }

    // ==================== 事件处理 ====================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.visible)
            return false;

        int scrollbarX = getX() + this.width - SCROLLBAR_WIDTH;
        int scrollAreaTop = getY() + PADDING;
        int scrollAreaHeight = this.height - PADDING * 2;

        if (event.button() == 0
                && event.x() >= scrollbarX && event.x() <= scrollbarX + SCROLLBAR_WIDTH
                && event.y() >= scrollAreaTop && event.y() <= scrollAreaTop + scrollAreaHeight) {

            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                float ratio = (float) getVisibleLineCount() / getDisplayItems().size();
                int thumbHeight = Math.max(20, (int) (scrollAreaHeight * ratio));
                float scrollRatio = (float) scrollOffset / maxScroll;
                int thumbY = scrollAreaTop + (int) ((scrollAreaHeight - thumbHeight) * scrollRatio);

                if (event.y() >= thumbY && event.y() <= thumbY + thumbHeight) {
                    this.draggingScrollbar = true;
                    this.dragStartY = event.y();
                    this.dragStartScrollOffset = this.scrollOffset;
                    return true;
                }
            }

            pageScroll(event.y() < (scrollAreaTop + scrollAreaHeight) / 2 ? -1 : 1);
            return true;
        }

        if (event.button() == 0) {
            int index = getEntryAt(event.x(), event.y());
            if (index >= 0 && index < getDisplayItems().size()) {
                selectedIndex = index;
                if (onItemSelected != null) {
                    onItemSelected.accept(getDisplayItems().get(index).id());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 处理滚动条拖拽
     */
    private void handleScrollbarDrag(double mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0)
            return;

        int scrollAreaTop = getY() + PADDING;
        int scrollAreaHeight = this.height - PADDING * 2;

        float ratio = (float) getVisibleLineCount() / getDisplayItems().size();
        int thumbHeight = Math.max(20, (int) (scrollAreaHeight * ratio));
        int effectiveHeight = scrollAreaHeight - thumbHeight;

        double deltaY = mouseY - this.dragStartY;
        float scrollRatio = effectiveHeight > 0 ? (float) deltaY / effectiveHeight : 0;
        int newScrollOffset = this.dragStartScrollOffset + (int) (maxScroll * scrollRatio);
        newScrollOffset = Math.max(0, Math.min(newScrollOffset, maxScroll));

        this.scrollOffset = newScrollOffset;
    }

    /**
     * 分页滚动
     */
    private void pageScroll(int direction) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0)
            return;

        int pageAmount = (int) ((this.height - PADDING * 2) * 0.8);
        this.scrollOffset += direction * pageAmount;
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!this.visible)
            return false;

        if (this.draggingScrollbar) {
            handleScrollbarDrag(event.y());
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!this.visible)
            return false;

        if (this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (getDisplayItems().size() <= getVisibleLineCount())
            return false;
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
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 先渲染边框和标题（子类实现）
        renderFrame(graphics);

        int x = getX();
        int y = getY();
        int visibleLines = getVisibleLineCount();

        // 检查是否需要显示滚动条
        boolean needsScrollbar = needsScrollbar();

        int oldHoveredIndex = hoveredIndex;
        hoveredIndex = getEntryAt(mouseX, mouseY);
        scrollbarHovered = needsScrollbar && isMouseOnScrollbar(mouseX, mouseY);

        if (hoveredIndex != oldHoveredIndex && onHover != null) {
            if (hoveredIndex >= 0 && hoveredIndex < getDisplayItems().size()) {
                onHover.accept(getDisplayItems().get(hoveredIndex));
            } else {
                onHover.accept(null);
            }
        }

        // 根据是否需要滚动条来决定裁剪区域
        int scissorRight = needsScrollbar ? x + this.width - PADDING - SCROLLBAR_WIDTH : x + this.width - PADDING;
        int contentBottom = y + this.height - 4;
        graphics.enableScissor(x + PADDING, y + PADDING, scissorRight, contentBottom);

        int textX = x + PADDING + 2;
        int textY = y + PADDING;

        for (int i = 0; i < visibleLines && (i + scrollOffset) < getDisplayItems().size(); i++) {
            int dataIndex = i + scrollOffset;
            int entryY = textY + i * LINE_HEIGHT;

            boolean isHovered = (dataIndex == hoveredIndex);
            boolean isSelected = (dataIndex == selectedIndex);

            int bgRight = needsScrollbar ? x + this.width - PADDING - SCROLLBAR_WIDTH : x + this.width - PADDING;
            if (isSelected) {
                graphics.fill(x + PADDING, entryY - 1, bgRight, entryY + LINE_HEIGHT - 1, selectedBg);
            } else if (isHovered) {
                graphics.fill(x + PADDING, entryY - 1, bgRight, entryY + LINE_HEIGHT - 1, hoverBg);
            }

            int color = isHovered ? hoverColor : textColor;
            graphics.text(this.font, getDisplayItems().get(dataIndex).name().text(), textX, entryY, color);
        }

        graphics.disableScissor();

        // 只在需要时渲染滚动条
        if (needsScrollbar) {
            renderScrollBar(graphics, x, y, visibleLines);
        }
    }

    /**
     * 渲染右侧滚动条
     */
    private void renderScrollBar(GuiGraphicsExtractor graphics, int x, int y, int visibleLines) {
        int sbX = x + this.width - SCROLLBAR_WIDTH;
        int sbTop = y + PADDING;
        int sbHeight = this.height - PADDING * 2;

        graphics.fill(sbX, sbTop, sbX + SCROLLBAR_WIDTH, sbTop + sbHeight, SCROLLBAR_BG);

        float ratio = (float) visibleLines / getDisplayItems().size();
        int thumbHeight = Math.max(20, (int) (sbHeight * ratio));
        float scrollRatio = getMaxScroll() > 0 ? (float) scrollOffset / getMaxScroll() : 0;
        int thumbY = sbTop + (int) ((sbHeight - thumbHeight) * scrollRatio);

        int thumbColor = scrollbarHovered ? SCROLLBAR_HOVER : SCROLLBAR_COLOR;
        graphics.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + thumbHeight, thumbColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // 无障碍：无需额外描述
    }
}
