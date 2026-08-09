package qdream.relay.client.screen.widget.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import qdream.relay.tools.TextTools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 可用操作列表 Widget
 * 支持滚动浏览、点击选中、悬停高亮
 * 点击某项操作时通过回调通知父级 Screen
 */
public class OperationListWidget extends AbstractWidget {

    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 3;
    private static final int HEADER_HEIGHT = 16;
    private static final int SCROLLBAR_WIDTH = 4;

    private static final int BG_COLOR = 0xFF1E1E1E;
    private static final int HEADER_BG = 0xFF252525;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int TEXT_COLOR = 0xFF00AA00;
    private static final int HOVER_COLOR = 0xFF55FF55;
    private static final int HOVER_BG = 0x3000FF00;
    private static final int SELECTED_BG = 0x4000AA00;
    private static final int SCROLLBAR_COLOR = 0xFF808080;
    private static final int SCROLLBAR_HOVER = 0xFF909090;
    private static final int SCROLLBAR_BG = 0xFF303030;

    private final Font font;
    private final List<String> operations;
    private Consumer<String> onOperationClicked;

    /** 悬停回调：当悬停在某个操作上时调用，参数为操作 ID */
    private Consumer<String> onHover;

    /** 缓存操作 ID 到显示名称的映射 */
    private final Map<String, String> displayNameCache = new HashMap<>();

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

    public OperationListWidget(int x, int y, int width, int height, Font font, List<String> operations) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.operations = operations;
    }

    public void setOnOperationClicked(Consumer<String> callback) {
        this.onOperationClicked = callback;
    }

    /**
     * 设置悬停回调
     */
    public void setOnHover(Consumer<String> callback) {
        this.onHover = callback;
    }

    /**
     * 获取操作的显示名称（从语言文件）
     */
    private String getDisplayName(String opId) {
        return displayNameCache.computeIfAbsent(opId, id -> TextTools.getName(id));
    }

    /** 获取可视区域内可显示的最大行数 */
    private int getVisibleLineCount() {
        return Math.max(1, (this.height - HEADER_HEIGHT - PADDING * 2) / LINE_HEIGHT);
    }

    /** 获取最大滚动偏移 */
    private int getMaxScroll() {
        return Math.max(0, operations.size() - getVisibleLineCount());
    }

    /** 根据鼠标坐标计算条目索引，超出范围返回 -1 */
    private int getEntryAt(double mouseX, double mouseY) {
        // 先检查鼠标是否在 Widget 范围内
        if (!isMouseOver(mouseX, mouseY))
            return -1;

        double relX = mouseX - (getX() + PADDING);
        double relY = mouseY - (getY() + HEADER_HEIGHT + PADDING);

        // 检查是否超出可视区域
        if (relX < 0 || relX > this.width - PADDING * 2 - 4 || relY < 0)
            return -1;

        int index = scrollOffset + (int) (relY / LINE_HEIGHT);
        if (index < 0 || index >= operations.size())
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
        if (!this.visible)
            return false;

        // 检查是否点击在滚动条区域
        int scrollbarX = getX() + this.width - SCROLLBAR_WIDTH;
        int scrollAreaTop = getY() + HEADER_HEIGHT + PADDING;
        int scrollAreaHeight = this.height - HEADER_HEIGHT - PADDING * 2;

        if (event.button() == 0
                && event.x() >= scrollbarX && event.x() <= scrollbarX + SCROLLBAR_WIDTH
                && event.y() >= scrollAreaTop && event.y() <= scrollAreaTop + scrollAreaHeight) {

            // 计算滑块位置，只有点击在滑块上才开始拖拽
            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                float ratio = (float) getVisibleLineCount() / operations.size();
                int thumbHeight = Math.max(20, (int) (scrollAreaHeight * ratio));
                float scrollRatio = (float) scrollOffset / maxScroll;
                int thumbY = scrollAreaTop + (int) ((scrollAreaHeight - thumbHeight) * scrollRatio);

                // 点击在滑块上：开始拖拽
                if (event.y() >= thumbY && event.y() <= thumbY + thumbHeight) {
                    this.draggingScrollbar = true;
                    this.dragStartY = event.y();
                    this.dragStartScrollOffset = this.scrollOffset;
                    return true;
                }
            }

            // 点击在轨道上：分页滚动，不设置 draggingScrollbar
            pageScroll(event.y() < (scrollAreaTop + scrollAreaHeight) / 2 ? -1 : 1);
            return true;
        }

        // 点击列表条目
        if (event.button() == 0) {
            int index = getEntryAt(event.x(), event.y());
            if (index >= 0 && index < operations.size()) {
                selectedIndex = index;
                if (onOperationClicked != null) {
                    onOperationClicked.accept(operations.get(index));
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

        int scrollAreaTop = getY() + HEADER_HEIGHT + PADDING;
        int scrollAreaHeight = this.height - HEADER_HEIGHT - PADDING * 2;

        // 计算滑块高度
        float ratio = (float) getVisibleLineCount() / operations.size();
        int thumbHeight = Math.max(20, (int) (scrollAreaHeight * ratio));
        int effectiveHeight = scrollAreaHeight - thumbHeight;

        // 计算鼠标移动距离
        double deltaY = mouseY - this.dragStartY;

        // 将鼠标移动距离转换为滚动偏移
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

        // 每次滚动可见区域的 80%
        int pageAmount = (int) ((this.height - HEADER_HEIGHT - PADDING * 2) * 0.8);
        this.scrollOffset += direction * pageAmount;
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!this.visible)
            return false;

        // 如果正在拖拽滚动条，根据鼠标的移动距离更新滚动
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

        // 停止拖拽滚动条
        if (this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (operations.size() <= getVisibleLineCount())
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
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int visibleLines = getVisibleLineCount();
        int listHeight = HEADER_HEIGHT + visibleLines * LINE_HEIGHT + PADDING * 2;

        // 背景
        graphics.fill(x, y, x + this.width, y + this.height, BG_COLOR);

        // 外边框
        graphics.outline(x, y, this.width, this.height, BORDER_COLOR);

        // 标题栏背景
        graphics.fill(x, y, x + this.width, y + HEADER_HEIGHT, HEADER_BG);
        graphics.horizontalLine(x, x + this.width, y + HEADER_HEIGHT, BORDER_COLOR);

        // 更新悬停索引
        int oldHoveredIndex = hoveredIndex;
        hoveredIndex = getEntryAt(mouseX, mouseY);
        scrollbarHovered = isMouseOnScrollbar(mouseX, mouseY);

        // 触发悬停回调
        if (hoveredIndex != oldHoveredIndex && onHover != null) {
            if (hoveredIndex >= 0 && hoveredIndex < operations.size()) {
                onHover.accept(operations.get(hoveredIndex));
            } else {
                onHover.accept(null);
            }
        }

        // 启用裁剪区域（底部留出计数提示的空间）
        int contentBottom = y + this.height - LINE_HEIGHT - 4;
        graphics.enableScissor(x + PADDING, y + HEADER_HEIGHT + PADDING,
                x + this.width - PADDING - 4, contentBottom);

        // 渲染列表条目
        int textX = x + PADDING + 2;
        int textY = y + HEADER_HEIGHT + PADDING;

        for (int i = 0; i < visibleLines && (i + scrollOffset) < operations.size(); i++) {
            int dataIndex = i + scrollOffset;
            String opId = operations.get(dataIndex);
            String displayName = getDisplayName(opId);
            int entryY = textY + i * LINE_HEIGHT;

            boolean isHovered = (dataIndex == hoveredIndex);
            boolean isSelected = (dataIndex == selectedIndex);

            // 选中背景优先于悬停背景
            if (isSelected) {
                graphics.fill(x + PADDING, entryY - 1, x + this.width - PADDING - 4, entryY + LINE_HEIGHT - 1,
                        SELECTED_BG);
            } else if (isHovered) {
                graphics.fill(x + PADDING, entryY - 1, x + this.width - PADDING - 4, entryY + LINE_HEIGHT - 1,
                        HOVER_BG);
            }

            int color = isHovered ? HOVER_COLOR : TEXT_COLOR;
            graphics.text(this.font, displayName, textX, entryY, color);
        }

        graphics.disableScissor();

        // 渲染滚动条（仅在内容超出时显示）
        if (operations.size() > visibleLines) {
            renderScrollBar(graphics, x, y, visibleLines);
        }

        // 标题文字
        graphics.text(this.font, Component.translatable("gui.relay:spell_editor.operations.title"), x + PADDING + 2,
                y + 4, 0xFF00FF00);

        // 底部计数提示（在裁剪区域外渲染，确保可见）
        int countY = y + this.height - LINE_HEIGHT - 2;
        graphics.text(this.font, Component.translatable("gui.relay:spell_editor.operations.count", operations.size()),
                x + PADDING, countY, 0xFF666666);
    }

    /**
     * 渲染右侧滚动条
     */
    private void renderScrollBar(GuiGraphicsExtractor graphics, int x, int y, int visibleLines) {
        int sbX = x + this.width - SCROLLBAR_WIDTH;
        int sbTop = y + HEADER_HEIGHT + PADDING;
        int sbHeight = this.height - HEADER_HEIGHT - PADDING * 2;

        // 滚动条背景
        graphics.fill(sbX, sbTop, sbX + SCROLLBAR_WIDTH, sbTop + sbHeight, SCROLLBAR_BG);

        // 滚动条滑块
        float ratio = (float) visibleLines / operations.size();
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
