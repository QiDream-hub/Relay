package qdream.relay.client.screen.widget.editor;

import com.google.gson.JsonParser;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * JSON 文本编辑器 Widget
 * 支持多行文本编辑、自动换行、滚动
 * 使用自定义的 CustomMultiLineEditBox 处理多行输入，支持精确光标控制
 */
public class JsonEditorWidget extends AbstractWidget {

    private static final int LINE_HEIGHT = 9;  // MultiLineEditBox 的行高
    private static final int PADDING = 4;
    private static final int SCROLLBAR_WIDTH = 4;

    private static final int BG_COLOR = 0xFF1A1A1A;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int ERROR_COLOR = 0xFFFF6B68;    // 错误红色
    private static final int SCROLLBAR_COLOR = 0xFF808080;
    private static final int SCROLLBAR_BG = 0xFF303030;

    private final Font font;
    private final CustomMultiLineEditBox editBox;

    /** 滚动偏移量（像素） */
    private int scrollOffset = 0;

    /** JSON 解析错误信息 */
    @Nullable
    private String errorMessage = null;

    /** 错误所在的行号 */
    private int errorLine = -1;

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
            false
        );
        
        // 监听文本变化，重新解析 JSON
        this.editBox.setValueListener(() -> this.parseJsonLines());
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
     * 是否有 JSON 解析错误
     */
    public boolean hasError() {
        return this.errorMessage != null;
    }

    /**
     * 获取错误信息
     */
    @Nullable
    public String getErrorMessage() {
        return this.errorMessage;
    }

    /**
     * 解析 JSON 文本检测错误
     */
    private void parseJsonLines() {
        this.errorMessage = null;
        this.errorLine = -1;

        String text = this.editBox.getValue();
        if (text.isEmpty()) {
            return;
        }

        try {
            // 尝试解析 JSON 验证语法
            JsonParser.parseString(text);
        } catch (Exception e) {
            // JSON 解析错误，显示错误信息
            this.errorMessage = e.getMessage();
            // 尝试从错误消息中提取行号
            this.errorLine = extractErrorLine(e.getMessage());
        }
    }

    /**
     * 从错误消息中提取行号
     */
    private int extractErrorLine(String message) {
        // Gson 错误消息格式："Expected value at line 5 column 10 path $[2]"
        if (message.contains("line ")) {
            try {
                String[] parts = message.split("line ");
                if (parts.length > 1) {
                    return Integer.parseInt(parts[1].split(" ")[0]) - 1;
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    /** 获取最大滚动偏移 */
    private int getMaxScroll() {
        int contentHeight = this.editBox.getInnerHeight();
        int visibleHeight = this.height - PADDING * 2;
        return Math.max(0, contentHeight - visibleHeight);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.visible) {
            return false;
        }

        // 检查是否点击在滚动条区域
        int scrollbarX = this.getX() + this.width - SCROLLBAR_WIDTH;
        if (event.x() >= scrollbarX && event.x() <= scrollbarX + SCROLLBAR_WIDTH) {
            this.handleScrollbarClick(event.y());
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
        // 确保聚焦
        if (!this.editBox.isFocused()) {
            this.editBox.setFocused(true);
        }
        return this.editBox.mouseDragged(event, deltaX, deltaY);
    }

    /**
     * 处理滚动条点击
     */
    private void handleScrollbarClick(double mouseY) {
        int maxScroll = this.getMaxScroll();
        if (maxScroll <= 0) {
            return;
        }

        float ratio = (float) (mouseY - this.getY() - PADDING) / (this.height - PADDING * 2);
        this.scrollOffset = (int) (maxScroll * Math.max(0, Math.min(1, ratio)));
        this.editBox.setScrollAmount(this.scrollOffset);
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

        // 渲染错误提示
        if (this.errorMessage != null) {
            this.renderErrorIndicator(graphics);
        }

        graphics.disableScissor();

        // 渲染滚动条
        if (this.getMaxScroll() > 0) {
            this.renderScrollBar(graphics);
        }
    }

    /**
     * 渲染错误指示器
     */
    private void renderErrorIndicator(GuiGraphicsExtractor graphics) {
        // 在底部显示错误消息
        String errorText = "JSON 错误：" + this.errorMessage;
        int errorWidth = this.font.width(errorText) + 6;
        int errorX = this.getX() + this.width - errorWidth - SCROLLBAR_WIDTH - 2;
        int errorY = this.getY() + this.height - LINE_HEIGHT - 2;

        // 错误背景
        graphics.fill(errorX - 2, errorY - 2, errorX + errorWidth, errorY + LINE_HEIGHT, 0x80FF0000);
        graphics.text(this.font, errorText, errorX, errorY, ERROR_COLOR);
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
