package qdream.relay.client.screen.widget.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Data;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.screen.SpellEditorScreenHandler;

import java.util.List;
import java.util.function.IntConsumer;

import com.google.gson.JsonObject;

/**
 * 程序列表 Widget
 * 显示当前编辑的程序（操作 ID 序列）
 * 支持滚动浏览、点击选中（用于删除）、悬停高亮
 * 选中变化时通过回调通知父级 Screen
 */
public class ProgramListWidget extends AbstractWidget {

    private static final int LINE_HEIGHT = 12;
    private static final int PADDING = 4;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SELECTED_COLOR = 0xFFFFFF00;
    private static final int SELECTED_BG = 0x40FFFF00;
    private static final int HOVER_COLOR = 0xFFFFCC00;
    private static final int HOVER_BG = 0x30FFFF00;
    private static final int SCROLLBAR_COLOR = 0xFF808080;
    private static final int SCROLLBAR_BG = 0xFF303030;

    private final Font font;
    private final List<Executable> program;
    private IntConsumer onSelectionChanged;

    /** 滚动偏移量（以行为单位） */
    private int scrollOffset = 0;

    /** 当前选中的条目索引，-1 表示未选中 */
    private int selectedIndex = -1;

    /** 当前鼠标悬停的条目索引，-1 表示无悬停 */
    private int hoveredIndex = -1;

    public ProgramListWidget(int x, int y, int width, int height, Font font, List<Executable> program) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.program = program;
    }

    public void setOnSelectionChanged(IntConsumer callback) {
        this.onSelectionChanged = callback;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void clearSelection() {
        selectedIndex = -1;
    }

    /** 获取可视区域内可显示的最大行数 */
    private int getVisibleLineCount() {
        return Math.max(1, (this.height - PADDING * 2) / LINE_HEIGHT);
    }

    /** 获取最大滚动偏移 */
    private int getMaxScroll() {
        return Math.max(0, program.size() - getVisibleLineCount());
    }

    /** 根据鼠标坐标计算条目索引，超出范围返回 -1 */
    private int getEntryAt(double mouseX, double mouseY) {
        double relX = mouseX - (getX() + PADDING);
        double relY = mouseY - (getY() + PADDING);
        if (relX < 0 || relX > this.width - PADDING * 2 || relY < 0)
            return -1;
        int index = scrollOffset + (int) (relY / LINE_HEIGHT);
        if (index < 0 || index >= program.size())
            return -1;
        return index;
    }

    // ==================== 事件处理 ====================

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int index = getEntryAt(event.x(), event.y());
            if (index >= 0 && index < program.size()) {
                // 点击已选中的条目取消选中，否则切换选中
                selectedIndex = (selectedIndex == index) ? -1 : index;
                if (onSelectionChanged != null) {
                    onSelectionChanged.accept(selectedIndex);
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

        for (int i = 0; i < visibleLines && (i + scrollOffset) < program.size(); i++) {
            int dataIndex = i + scrollOffset;
            Executable entry = program.get(dataIndex);
            int entryY = textY + i * LINE_HEIGHT;

            boolean isSelected = (dataIndex == selectedIndex);
            boolean isHovered = (dataIndex == hoveredIndex);

            // 选中背景
            if (isSelected) {
                graphics.fill(x + PADDING, entryY - 1, x + this.width - PADDING, entryY + LINE_HEIGHT - 1, SELECTED_BG);
            }
            // 悬停背景（仅在未选中时显示）
            else if (isHovered) {
                graphics.fill(x + PADDING, entryY - 1, x + this.width - PADDING, entryY + LINE_HEIGHT - 1, HOVER_BG);
            }

            int color;
            if (isSelected) {
                color = SELECTED_COLOR;
            } else if (isHovered) {
                color = HOVER_COLOR;
            } else {
                color = TEXT_COLOR;
            }

            // 构建显示文本
            String label;
            if (entry instanceof Spell spell) {
                label = '[' + spell.getId() + ']';
            } else if (entry instanceof Data data) {
                JsonObject json = new JsonObject();
                data.toJson(json);
                label = '[' + data.getId() + "]:" + json.get("data");
            } else {
                label = "[?]";
            }
            graphics.text(this.font, label, textX, entryY, color);
        }

        graphics.disableScissor();

        // 渲染滚动条（仅在内容超出时显示）
        if (program.size() > visibleLines) {
            renderScrollBar(graphics, x, y, visibleLines);
        }
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
        float ratio = (float) visibleLines / program.size();
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
