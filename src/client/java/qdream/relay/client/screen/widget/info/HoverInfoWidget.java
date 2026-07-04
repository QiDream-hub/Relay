package qdream.relay.client.screen.widget.info;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 悬停信息提示 Widget
 * 用于在鼠标悬停时显示操作或类型的详细信息
 * 布局分为两部分：
 * - 第一部分：标题 + 描述
 * - 第二部分：输入/输出签名
 */
public class HoverInfoWidget extends net.minecraft.client.gui.components.AbstractWidget {

    private static final int BG_COLOR = 0xFF1E1E1E;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int TITLE_COLOR = 0xFF00FF00;
    private static final int DESC_COLOR = 0xFFCCCCCC;
    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int TYPE_COLOR = 0xFF55FF55;

    private final Font font;

    /** 要显示的信息行 */
    private InfoContent content;

    public HoverInfoWidget(int x, int y, int width, int height, Font font) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.content = null;
    }

    /**
     * 设置要显示的内容
     */
    public void setContent(InfoContent content) {
        this.content = content;
    }

    /**
     * 清空内容
     */
    public void clearContent() {
        this.content = null;
    }

    /**
     * 检查是否有内容
     */
    public boolean hasContent() {
        return content != null;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (content == null) {
            return;
        }

        int x = getX();
        int y = getY();

        // 背景
        graphics.fill(x, y, x + this.width, y + this.height, BG_COLOR);

        // 外边框
        graphics.outline(x, y, this.width, this.height, BORDER_COLOR);

        // 渲染内容
        int textX = x + 6;
        int textY = y + 6;
        int lineHeight = 10;

        // 第一部分：标题
        if (content.title != null && !content.title.isEmpty()) {
            graphics.text(this.font, content.title, textX, textY, TITLE_COLOR);
            textY += lineHeight + 4;
        }

        // 第二部分：描述
        if (content.description != null && !content.description.isEmpty()) {
            // 描述文字自动换行
            var wrappedLines = font.split(
                    Component.literal(content.description),
                    this.width - 12);
            for (var line : wrappedLines) {
                graphics.text(this.font, line, textX, textY, DESC_COLOR);
                textY += lineHeight + 1;
            }
            textY += 2; // 描述后额外间距
        }

        // 第三部分：输入/输出签名
        if (content.lines != null && !content.lines.isEmpty()) {
            // 在描述和签名之间添加分隔线
            int separatorY = textY - 2;
            graphics.horizontalLine(x + 4, x + this.width - 4, separatorY, BORDER_COLOR);
            textY += 4;

            for (InfoLine line : content.lines) {
                var wrappedLines = font.split(Component.literal(line.text), this.width - 12);
                for (var displayLine : wrappedLines) {
                    int color = line.color != null ? line.color : DESC_COLOR;
                    graphics.text(this.font, displayLine, textX, textY, color);
                    textY += lineHeight + 2;
                }
            }
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // 信息提示 Widget 不拦截鼠标事件，让下层 Widget 能正常接收
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // 无障碍：无需额外描述
    }

    /**
     * 信息内容
     *
     * @param title       标题
     * @param description 描述
     * @param lines       内容行列表（输入/输出签名）
     */
    public record InfoContent(String title, String description, List<InfoLine> lines) {
        public InfoContent(String title, String description) {
            this(title, description, new ArrayList<>());
        }

        public static InfoContent of(String title, String description) {
            return new InfoContent(title, description);
        }

        public InfoContent addLine(String text) {
            lines.add(new InfoLine(text, null));
            return this;
        }

        public InfoContent addLine(String text, int color) {
            lines.add(new InfoLine(text, color));
            return this;
        }
    }

    /**
     * 信息行
     * 
     * @param text  文本内容
     * @param color 颜色（可选）
     */
    public record InfoLine(String text, Integer color) {
    }
}
