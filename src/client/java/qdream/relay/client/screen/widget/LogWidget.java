package qdream.relay.client.screen.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 日志窗口 Widget
 * 用于显示 Shell 的调试日志输出
 * 支持鼠标滚轮翻页查看历史日志
 * 支持自动换行
 */
public class LogWidget extends AbstractWidget {

    private final Font font;
    private final Supplier<List<Component>> logSupplier;

    // 可视行数
    private int maxVisibleLines;
    // 滚动偏移量（0 表示无偏移，正数表示向上滚动查看更旧的日志）
    private int scrollOffset;
    // 可视内容
    private Supplier<List<FormattedCharSequence>> visibleLogSupplier;

    // 布局常量
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 8;
    private static final int TITLE_HEIGHT = 10;

    public LogWidget(int x, int y, int width, int height, Font font, Supplier<List<Component>> logSupplier) {
        super(x, y, width, height, Component.literal(""));
        this.font = font;
        this.logSupplier = logSupplier;
        this.scrollOffset = 0;
        // 计算最大可见行数
        this.maxVisibleLines = (height - PADDING * 2 - TITLE_HEIGHT - PADDING) / LINE_HEIGHT;
        // visibleLogSupplier 每次调用时获取最新的日志并截取
        this.visibleLogSupplier = () -> {
            List<Component> allLogs = logSupplier.get();
            if (allLogs == null || allLogs.isEmpty()) {
                return Collections.emptyList();
            }

            // 计算总行数（考虑自动换行）
            int totalLines = 0;
            List<FormattedCharSequence> allLines = new ArrayList<>();
            for (Component log : allLogs) {
                List<FormattedCharSequence> lines = font.split(log, width);
                allLines.addAll(lines);
                totalLines += lines.size();
            }

            // 计算可视窗口的起始和结束索引
            // scrollOffset = 0 时显示最新的日志（底部对齐）
            // scrollOffset > 0 时向上滚动，显示更旧的日志
            int endIndex = totalLines; // 最新日志的下一行（开区间）
            int startIndex = Math.max(0, endIndex - maxVisibleLines - scrollOffset);
            endIndex = Math.min(totalLines, startIndex + maxVisibleLines);

            // 截取可视窗口内的行
            return allLines.subList(startIndex, endIndex);
        };
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        List<FormattedCharSequence> list = visibleLogSupplier.get();
        // 当前文本y
        int currentY = this.getY();
        for (FormattedCharSequence formattedCharSequence : list) {
            graphics.text(font, formattedCharSequence, this.getX(), currentY, 0xFFFFFFFF);
            currentY += LINE_HEIGHT;
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!isMouseOver(x, y)) {
            return false;
        }

        // 计算总行数（考虑自动换行）
        List<Component> allLogs = logSupplier.get();
        int totalLines = 0;
        if (allLogs != null) {
            for (Component log : allLogs) {
                totalLines += Math.max(1, font.split(log, width).size());
            }
        }

        // 计算最大可滚动偏移量（最多能向上滚动多少行）
        int maxScrollOffset = Math.max(0, totalLines - maxVisibleLines);

        // 更新滚动偏移量：scrollY > 0 表示向上滚动（查看更旧的日志）
        this.scrollOffset += (int) scrollY;

        // 限制滚动范围：[0, maxScrollOffset]
        // 0 = 显示最新日志（底部对齐），maxScrollOffset = 滚动到最旧的日志
        this.scrollOffset = Math.min(maxScrollOffset, Math.max(0, this.scrollOffset));

        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // 无障碍：无需额外描述
    }
}
