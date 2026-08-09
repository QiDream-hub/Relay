package qdream.relay.client.screen.widget.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import qdream.relay.client.screen.widget.editor.tools.InfoContent;
import qdream.relay.client.screen.widget.editor.tools.InfoContent.InfoLine;

/**
 * 悬停信息提示 Widget
 * 用于在鼠标悬停时显示操作或类型的详细信息
 * 布局分为两部分：
 * - 第一部分：标题 + 描述
 * - 第二部分：输入/输出签名
 */
public class HoverInfoWidget extends AbstractWidget {

    private static final int BG_COLOR = 0xFF1E1E1E;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int TITLE_COLOR = 0xFF00FF00;
    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int TYPE_COLOR = 0xFF55FF55;

    // 语言文件键
    private static final String COST_LABEL_KEY = "gui.relay:spell_editor.info.cost_label";
    private static final String ENERGY_COST_LABEL_KEY = "gui.relay:spell_editor.info.energy_cost_label";

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

        // 第一部分：名称与资源消耗
        graphics.text(this.font, content.name().text(), textX, textY, content.name().color());
        textY += lineHeight + 1;

        // 使用语言文件显示资源消耗标签
        Integer cost = content.cost();
        Double energyCost = content.energyCost();
        if (cost != null || energyCost != null) {
            String costLabel = Component.translatable(COST_LABEL_KEY).getString();
            String energyCostLabel = Component.translatable(ENERGY_COST_LABEL_KEY).getString();
            graphics.text(this.font, costLabel + cost + "  " + energyCostLabel + energyCost, textX,
                    textY,
                    content.name().color());
            textY += lineHeight + 1;
        }
        graphics.horizontalLine(x + 4, x + this.width - 4, textY, BORDER_COLOR);
        textY += 3;

        // 第二部分：描述
        String description = content.description().text();
        if (description != null && !description.isEmpty()) {
            // 描述文字自动换行
            var wrappedLines = font.split(
                    Component.literal(description),
                    this.width - 12);
            for (var line : wrappedLines) {
                graphics.text(this.font, line, textX, textY, content.description().color());
                textY += lineHeight + 1;
            }
            textY += 2; // 描述后额外间距
        }

        // 第三部分：输入/输出签名
        if (content.signature() != null && !content.signature().isEmpty()) {

            for (InfoLine line : content.signature()) {
                var wrappedLines = font.split(Component.literal(line.text()), this.width - 12);
                for (var displayLine : wrappedLines) {
                    graphics.text(this.font, displayLine, textX, textY, line.color());
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

}
