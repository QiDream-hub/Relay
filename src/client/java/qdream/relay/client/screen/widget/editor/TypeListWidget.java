package qdream.relay.client.screen.widget.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import qdream.relay.client.screen.widget.ScrollableListWidget;
import qdream.relay.client.screen.widget.editor.tools.InfoContent;

import java.util.List;
import java.util.function.Consumer;

/**
 * 数据类型列表 Widget
 * 显示可用的 Iota 数据类型（如 string, number, boolean 等）
 * 支持滚动浏览、点击添加、悬停高亮
 * 点击某类型时通过回调通知父级添加该类型
 */
public class TypeListWidget extends AbstractWidget {

    private static final int HEADER_HEIGHT = 16;
    private static final int PADDING = 3;

    private static final int BG_COLOR = 0xFF1E1E1E;
    private static final int HEADER_BG = 0xFF252525;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int TEXT_COLOR = 0xFFAAAAFF;
    private static final int HOVER_COLOR = 0xFFCCCCFF;
    private static final int HOVER_BG = 0x308080FF;
    private static final int SELECTED_BG = 0x408080FF;

    private final ScrollableListWidget listWidget;
    private final Font font;
    private final List<InfoContent> dataTypes;

    public TypeListWidget(int x, int y, int width, int height, Font font, List<InfoContent> dataTypes) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.dataTypes = dataTypes;
        // 列表区域扣除标题栏高度
        this.listWidget = new ScrollableListWidget(x, y + HEADER_HEIGHT, width, height - HEADER_HEIGHT, font,
                dataTypes);
        this.listWidget.setColors(TEXT_COLOR, HOVER_COLOR, HOVER_BG, SELECTED_BG);
    }

    public void setOnTypeClicked(Consumer<String> callback) {
        this.listWidget.setOnItemSelected(callback);
    }

    public void setOnHover(Consumer<InfoContent> callback) {
        this.listWidget.setOnHover(callback);
    }

    /**
     * 获取当前选中的类型索引，-1 表示无选中
     */
    public int getSelectedIndex() {
        return this.listWidget.getSelectedIndex();
    }

    /**
     * 获取当前选中的类型 ID
     *
     * @return 选中的类型 ID，无选中时返回 null
     */
    public String getSelectedType() {
        int index = this.listWidget.getSelectedIndex();
        if (index >= 0 && index < dataTypes.size()) {
            return dataTypes.get(index).id();
        }
        return null;
    }

    /**
     * 获取当前悬停的类型索引，-1 表示无悬停
     */
    public int getHoveredIndex() {
        return this.listWidget.getHoveredIndex();
    }

    /**
     * 获取当前悬停的类型 ID
     *
     * @return 悬停的类型 ID，无悬停时返回 null
     */
    public String getHoveredType() {
        int index = this.listWidget.getHoveredIndex();
        if (index >= 0 && index < dataTypes.size()) {
            return dataTypes.get(index).id();
        }
        return null;
    }

    /**
     * 获取滚动偏移量
     */
    public int getScrollOffset() {
        return this.listWidget.getScrollOffset();
    }

    /**
     * 设置滚动偏移量
     */
    public void setScrollOffset(int offset) {
        this.listWidget.setScrollOffset(offset);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return this.listWidget.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return this.listWidget.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return this.listWidget.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return this.listWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 背景
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, BG_COLOR);

        // 外边框
        graphics.outline(this.getX(), this.getY(), this.width, this.height, BORDER_COLOR);

        // 标题栏背景和分隔线
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + HEADER_HEIGHT, HEADER_BG);
        graphics.horizontalLine(this.getX(), this.getX() + this.width, this.getY() + HEADER_HEIGHT, BORDER_COLOR);

        // 标题文字
        graphics.text(this.font, Component.translatable("gui.relay:spell_editor.types.title"),
                this.getX() + PADDING + 2, this.getY() + 4, 0xFFAAAAFF);

        // 渲染列表内容（由内部组件负责）
        this.listWidget.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // 无障碍：无需额外描述
    }
}
