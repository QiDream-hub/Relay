package qdream.relay.client.screen.widget.info;

import java.util.ArrayList;
import java.util.List;

/**
 * 悬停信息提示 Widget
 * 用于在鼠标悬停时显示操作或类型的详细信息
 */
public class HoverInfoWidget extends net.minecraft.client.gui.components.AbstractWidget {

    private static final int BG_COLOR = 0xFF1E1E1E;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int TEXT_COLOR = 0xFFCCCCCC;

    private final net.minecraft.client.gui.Font font;

    /** 要显示的信息行 */
    private InfoContent content;

    public HoverInfoWidget(int x, int y, int width, int height, net.minecraft.client.gui.Font font) {
        super(x, y, width, height, net.minecraft.network.chat.Component.empty());
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
        return content != null && !content.lines.isEmpty();
    }

    @Override
    protected void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (content == null || content.lines.isEmpty()) {
            return;
        }

        int x = getX();
        int y = getY();

        // 背景
        graphics.fill(x, y, x + this.width, y + this.height, BG_COLOR);

        // 外边框
        graphics.outline(x, y, this.width, this.height, BORDER_COLOR);

        // 渲染内容行
        int textX = x + 6;
        int textY = y + 6;
        int lineHeight = 10;

        for (InfoLine line : content.lines) {
            int color = line.color != null ? line.color : TEXT_COLOR;
            graphics.text(this.font, line.text, textX, textY, color);
            textY += lineHeight + 2;
        }
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput builder) {
        // 无障碍：无需额外描述
    }

    /**
     * 信息内容
     * @param title 标题
     * @param description 描述
     * @param lines 内容行列表
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
     * @param text 文本内容
     * @param color 颜色（可选）
     */
    public record InfoLine(String text, Integer color) {
    }
}
