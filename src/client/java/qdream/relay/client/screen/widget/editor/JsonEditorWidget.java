package qdream.relay.client.screen.widget.editor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.llamalad7.mixinextras.lib.apache.commons.StringUtils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * JSON 文本编辑器 Widget
 * 支持多行文本编辑、自动换行、滚动
 * 使用自定义的 CustomMultiLineEditBox 处理多行输入，支持精确光标控制
 */
public class JsonEditorWidget extends AbstractWidget {

    private static final int LINE_HEIGHT = 9; // MultiLineEditBox 的行高
    private static final int PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 4;

    private static final int BG_COLOR = 0xFF1A1A1A;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SCROLLBAR_COLOR = 0xFF808080;
    private static final int SCROLLBAR_BG = 0xFF303030;

    private final Font font;
    private final CustomMultiLineEditBox editBox;

    /** 滚动偏移量（像素） */
    private int scrollOffset = 0;

    /** 是否在拖拽滚动条 */
    private boolean draggingScrollbar = false;

    /** 拖动滚动条时的初始鼠标 Y 位置 */
    private double dragStartY = 0;

    /** 拖动滚动条时的初始滚动偏移 */
    private int dragStartScrollOffset = 0;

    // 错误记录
    private List<Component> logList = null;

    public JsonEditorWidget(int x, int y, int width, int height, Font font) {
        super(x, y, width, height, Component.literal("JSON Editor"));
        this.font = font;

        // 使用自定义的 CustomMultiLineEditBox 支持精确光标控制
        this.editBox = new CustomMultiLineEditBox(
                font,
                x + PADDING,
                y + PADDING,
                width - PADDING * 2 - SCROLLBAR_WIDTH,
                height - PADDING * 2,
                Component.empty(),
                TEXT_COLOR,
                false,
                TEXT_COLOR,
                false,
                false);

        // 监听文本变化，重新解析 JSON
        this.editBox.setValueListener(() -> this.parseJsonLines());
    }

    // 获取错误记录
    public List<Component> getErrorlog() {
        return logList;
    }

    /**
     * 设置 JSON 内容（从 List<Executable> 序列化而来）
     */
    public void setJsonContent(String json) {
        this.editBox.setValue(json);
        this.parseJsonLines();
        this.scrollOffset = 0;
    }

    /**
     * 获取当前 JSON 内容
     */
    public String getJsonContent() {
        return this.editBox.getValue();
    }

    /**
     * 解析 JSON 文本检测错误
     */
    private void parseJsonLines() {
        logList = null;
        String text = this.editBox.getValue();
        if (text.isEmpty()) {
            return;
        }

        try {
            // 尝试解析 JSON 验证语法
            JsonParser.parseString(text);
        } catch (JsonSyntaxException e) {
            // 错误坐标
            JsonErrorPosition errorPosition = extractErrorPosition(e.getMessage());
            logList = List.of(
                    Component.translatable("error.json_edit_parse.error_position"),
                    Component.translatable("error.json_edit_parse.error_line", errorPosition.getLine()),
                    Component.translatable("error.json_edit_parse.error_column", errorPosition.getColumn()));
        } catch (Exception e) {
            logList = List.of(Component.translatable("error.json_edit_parse.unknown", e.getMessage()));
        }
    }

    /**
     * 从 Gson 错误消息中提取行号和列号
     * 
     * @param message Gson 抛出的异常消息，格式示例：
     *                "Expected value at line 5 column 10 path $[2]"
     *                "Expected BEGIN_OBJECT but was STRING at line 3 column 5 path
     *                $.data.user"
     * @return JsonErrorPosition 对象，包含行号、列号和路径信息
     */
    private JsonErrorPosition extractErrorPosition(String message) {
        if (StringUtils.isEmpty(message)) {
            return JsonErrorPosition.EMPTY;
        }

        int line = -1;
        int column = -1;
        String path = null;

        try {
            // 提取行号
            Pattern linePattern = Pattern.compile("line\\s+(\\d+)");
            Matcher lineMatcher = linePattern.matcher(message);
            if (lineMatcher.find()) {
                line = Integer.parseInt(lineMatcher.group(1));
            }

            // 提取列号
            Pattern columnPattern = Pattern.compile("column\\s+(\\d+)");
            Matcher columnMatcher = columnPattern.matcher(message);
            if (columnMatcher.find()) {
                column = Integer.parseInt(columnMatcher.group(1));
            }

            // 提取 JSON Path（可选）
            Pattern pathPattern = Pattern.compile("path\\s+([^\\s]+)");
            Matcher pathMatcher = pathPattern.matcher(message);
            if (pathMatcher.find()) {
                path = pathMatcher.group(1);
            }

        } catch (Exception e) {
            // 解析失败时返回默认值
            return JsonErrorPosition.EMPTY;
        }

        return new JsonErrorPosition(line, column, path);
    }

    /**
     * 错误位置信息类
     */
    public static class JsonErrorPosition {
        public static final JsonErrorPosition EMPTY = new JsonErrorPosition(-1, -1, null);

        private final int line;
        private final int column;
        private final String path;

        public JsonErrorPosition(int line, int column, String path) {
            this.line = line;
            this.column = column;
            this.path = path;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        public String getPath() {
            return path;
        }

        /**
         * 获取 0 基索引的行号（便于在代码中使用）
         */
        public int getZeroBasedLine() {
            return line > 0 ? line - 1 : -1;
        }

        /**
         * 获取 0 基索引的列号（便于在代码中使用）
         */
        public int getZeroBasedColumn() {
            return column > 0 ? column - 1 : -1;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (line > 0) {
                sb.append("行: ").append(line);
            }
            if (column > 0) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append("列: ").append(column);
            }
            if (path != null) {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append("路径: ").append(path);
            }
            return sb.toString();
        }
    }

    /** 获取最大滚动偏移 */
    private int getMaxScroll() {
        int contentHeight = this.editBox.getInnerHeight();
        int visibleHeight = this.height - PADDING * 2;
        return Math.max(0, contentHeight - visibleHeight);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.draggingScrollbar = false;
        if (!this.visible) {
            return false;
        }

        // 检查是否点击在滚动条区域
        int scrollbarX = this.getX() + this.width - SCROLLBAR_WIDTH;
        int scrollAreaTop = this.getY() + PADDING;
        int scrollAreaHeight = this.height - PADDING * 2;

        if (event.x() >= scrollbarX
                && event.x() <= scrollbarX + SCROLLBAR_WIDTH
                && event.y() >= scrollAreaTop
                && event.y() <= scrollAreaTop + scrollAreaHeight) {

            // 计算滑块位置，检查是否点击在滑块上
            int maxScroll = this.getMaxScroll();
            if (maxScroll > 0) {
                int contentHeight = this.editBox.getInnerHeight();
                float ratio = (float) (this.height - PADDING * 2) / contentHeight;
                int thumbHeight = Math.max(20, (int) (scrollAreaHeight * ratio));
                float scrollRatio = (float) this.scrollOffset / maxScroll;
                int thumbY = scrollAreaTop + (int) ((scrollAreaHeight - thumbHeight) * scrollRatio);

                // 只有点击在滑块上才开始拖拽
                if (event.y() >= thumbY && event.y() <= thumbY + thumbHeight) {
                    this.draggingScrollbar = true;
                    this.dragStartY = event.y();
                    this.dragStartScrollOffset = this.scrollOffset;
                    return true;
                }
            }

            // 点击轨道：分页滚动
            this.pageScroll(event.y() < (scrollAreaTop + scrollAreaHeight) / 2 ? -1 : 1);
            return true;
        }

        // 先设置聚焦，再转发给 EditBox
        this.setFocused(true);
        return this.editBox.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.visible) {
            return false;
        }
        // 确保聚焦
        if (!this.editBox.isFocused()) {
            this.editBox.setFocused(true);
        }
        // MultiLineEditBox 已经原生支持 Enter 键换行
        return this.editBox.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!this.visible) {
            return false;
        }
        // 确保聚焦
        if (!this.editBox.isFocused()) {
            this.editBox.setFocused(true);
        }
        return this.editBox.charTyped(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.visible) {
            return false;
        }

        int maxScroll = this.getMaxScroll();
        if (maxScroll <= 0) {
            return false;
        }

        this.scrollOffset -= (int) (scrollY * 10);
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));
        this.editBox.setScrollAmount(this.scrollOffset);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!this.visible) {
            return false;
        }

        // 如果正在拖拽滚动条，根据鼠标的移动距离更新滚动位置
        if (this.draggingScrollbar) {
            this.handleScrollbarDrag(event.y());
            return true;
        }

        // 只有在 Widget 范围内才转发拖动事件给编辑框（用于文本选择）
        if (event.x() >= this.getX() && event.x() <= this.getX() + this.width
                && event.y() >= this.getY() && event.y() <= this.getY() + this.height) {
            // 确保聚焦
            if (!this.editBox.isFocused()) {
                this.editBox.setFocused(true);
            }
            return this.editBox.mouseDragged(event, deltaX, deltaY);
        }

        return false;
    }

    /**
     * 处理滚动条拖拽
     */
    private void handleScrollbarDrag(double mouseY) {
        int maxScroll = this.getMaxScroll();
        if (maxScroll <= 0) {
            return;
        }

        // 滚动条的有效区域：从 getY() + PADDING 到 getY() + height - PADDING
        int scrollAreaTop = this.getY() + PADDING;
        int scrollAreaHeight = this.height - PADDING * 2;

        // 计算滑块可移动的有效高度（减去滑块自身高度）
        int contentHeight = this.editBox.getInnerHeight();
        float ratio = (float) (this.height - PADDING * 2) / contentHeight;
        int thumbHeight = Math.max(20, (int) (scrollAreaHeight * ratio));
        int effectiveHeight = scrollAreaHeight - thumbHeight;

        // 计算鼠标移动距离
        double deltaY = mouseY - this.dragStartY;

        // 将鼠标移动距离转换为滚动偏移
        float scrollRatio = effectiveHeight > 0 ? (float) deltaY / effectiveHeight : 0;
        int newScrollOffset = this.dragStartScrollOffset + (int) (maxScroll * scrollRatio);
        newScrollOffset = Math.max(0, Math.min(newScrollOffset, maxScroll));

        this.scrollOffset = newScrollOffset;
        this.editBox.setScrollAmount(this.scrollOffset);
    }

    /**
     * 分页滚动
     */
    private void pageScroll(int direction) {
        int maxScroll = this.getMaxScroll();
        if (maxScroll <= 0) {
            return;
        }

        // 每次滚动可见区域的 80%
        int pageAmount = (int) ((this.height - PADDING * 2) * 0.8);
        this.scrollOffset += direction * pageAmount;
        this.scrollOffset = Math.max(0, Math.min(this.scrollOffset, maxScroll));
        this.editBox.setScrollAmount(this.scrollOffset);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!this.visible) {
            return false;
        }

        // 停止拖拽滚动条
        if (this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }

        return this.editBox.mouseReleased(event);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        this.editBox.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return this.editBox.isFocused();
    }

    /**
     * 在光标位置插入文本
     */
    public void insertAtCursor(String text) {
        this.editBox.insertText(text);
    }

    /**
     * 在光标位置插入文本并自动追加逗号
     * 插入后光标位于逗号之后，不选中任何内容
     * 
     * @param textToInsert 要插入的文本（不含逗号）
     */
    public void insertWithComma(String textToInsert) {
        this.editBox.insertWithComma(textToInsert);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();

        // 背景
        graphics.fill(x, y, x + this.width, y + this.height, BG_COLOR);
        graphics.outline(x, y, this.width, this.height, BORDER_COLOR);

        // 启用裁剪区域
        graphics.enableScissor(x + PADDING, y + PADDING,
                x + this.width - PADDING - SCROLLBAR_WIDTH, y + this.height - PADDING);

        // 让 CustomMultiLineEditBox 渲染文本和光标
        this.editBox.extractWidgetRenderState(graphics, mouseX, mouseY, delta);

        graphics.disableScissor();

        // 渲染滚动条
        if (this.getMaxScroll() > 0) {
            this.renderScrollBar(graphics);
        }
    }

    /**
     * 渲染滚动条
     */
    private void renderScrollBar(GuiGraphicsExtractor graphics) {
        int sbX = this.getX() + this.width - SCROLLBAR_WIDTH;
        int sbTop = this.getY() + PADDING;
        int sbHeight = this.height - PADDING * 2;

        // 滚动条背景
        graphics.fill(sbX, sbTop, sbX + SCROLLBAR_WIDTH, sbTop + sbHeight, SCROLLBAR_BG);

        // 滚动条滑块
        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            int contentHeight = this.editBox.getInnerHeight();
            float ratio = (float) (this.height - PADDING * 2) / contentHeight;
            int thumbHeight = Math.max(20, (int) (sbHeight * ratio));
            float scrollRatio = (float) this.scrollOffset / maxScroll;
            int thumbY = sbTop + (int) ((sbHeight - thumbHeight) * scrollRatio);

            graphics.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_COLOR);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // 无障碍：无需额外描述
    }
}
