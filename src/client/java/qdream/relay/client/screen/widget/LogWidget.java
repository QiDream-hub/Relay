package qdream.relay.client.screen.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 日志窗口 Widget
 * 用于显示 Shell 的调试日志输出
 * 支持滚动查看历史日志
 * 支持自动换行
 */
public class LogWidget extends AbstractWidget {

    private static final int BG_COLOR = 0xFF0A0A15;
    private static final int BORDER_COLOR = 0xFF303050;
    private static final int TITLE_COLOR = 0xFF8888FF;
    private static final int DEFAULT_COLOR = 0xFFAAAAAA;
    private static final int MISHAP_COLOR = 0xFFFF5555;
    private static final int BEFORE_STEP_COLOR = 0xFF55FF55;
    private static final int AFTER_STEP_COLOR = 0xFF55FFFF;
    private static final int EMPTY_COLOR = 0xFF555555;

    private final Font font;
    private final Supplier<List<String>> logSupplier;

    // 滚动位置
    private int scrollOffset = 0;
    private int maxVisibleLines;

    // 布局常量
    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 8;
    private static final int TITLE_HEIGHT = 10;

    // 缓存换行后的日志
    private List<LogLine> cachedLogLines = new ArrayList<>();
    private List<String> lastLogs;

    public LogWidget(int x, int y, int width, int height, Font font, Supplier<List<String>> logSupplier) {
        super(x, y, width, height, Component.literal("日志窗口"));
        this.font = font;
        this.logSupplier = logSupplier;
        // 计算最大可见行数
        this.maxVisibleLines = (height - PADDING * 2 - TITLE_HEIGHT - PADDING) / LINE_HEIGHT;
    }

    /**
     * 日志行（支持换行）
     */
    private static class LogLine {
        final String text;
        final int color;
        final int lineCount;

        LogLine(String text, int color, Font font, int maxWidth) {
            this.text = text;
            this.color = color;
            this.lineCount = font.split(Component.literal(text), maxWidth).size();
        }

        int getLineCount() {
            return lineCount;
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 检查日志是否为空，空则不渲染
        List<String> logs = logSupplier.get();
        if (logs == null || logs.isEmpty()) {
            cachedLogLines.clear();
            lastLogs = null;
            return;
        }

        int x = getX();
        int y = getY();

        // 日志内容区域
        int textY = y + PADDING + TITLE_HEIGHT + 2;
        int textWidth = this.width - PADDING * 2;

        if (lastLogs == null || !lastLogs.equals(logs)) {
            lastLogs = logs;
            cachedLogLines = new ArrayList<>();
            for (String log : logs) {
                cachedLogLines.add(new LogLine(log, getLogColor(log), font, textWidth));
            }
        }

        // 计算总行数（换行后）
        int totalLines = cachedLogLines.stream().mapToInt(LogLine::getLineCount).sum();

        // 计算要显示的日志范围
        int startIndex = Math.max(0, totalLines - maxVisibleLines - scrollOffset);
        int displayedLines = 0;
        int currentY = textY;

        for (LogLine logLine : cachedLogLines) {
            if (displayedLines >= totalLines - startIndex) {
                break;
            }

            // 渲染时进行换行
            var wrappedLines = font.split(Component.literal(logLine.text), textWidth);
            for (int lineIdx = 0; lineIdx < wrappedLines.size(); lineIdx++) {
                if (displayedLines >= startIndex) {
                    if (currentY < y + this.height - PADDING) {
                        var wrappedText = wrappedLines.get(lineIdx);
                        graphics.text(this.font, wrappedText, x + PADDING, currentY, logLine.color);
                        currentY += LINE_HEIGHT;
                    }
                }
                displayedLines++;
            }
        }
    }

    /**
     * 根据日志内容获取颜色
     */
    private int getLogColor(String log) {
        if (log.contains("mishap")) {
            return MISHAP_COLOR; // 红色 - 事故
        } else if (log.contains("beforeStep")) {
            return BEFORE_STEP_COLOR; // 绿色 - 执行前
        } else if (log.contains("afterStep")) {
            return AFTER_STEP_COLOR; // 青色 - 执行后
        }
        return DEFAULT_COLOR; // 灰色 - 默认
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // 检测鼠标是否在日志窗口内，以便处理滚轮事件
        return mouseX >= getX() && mouseX < getX() + this.width &&
               mouseY >= getY() && mouseY < getY() + this.height;
    }

    /**
     * 处理滚轮事件 - 滚动日志
     * @param verticalAmount 垂直滚动量（正数向下，负数向上）
     * @return 如果处理了滚动事件返回 true
     */
    public boolean handleScroll(double verticalAmount) {
        List<String> logs = logSupplier.get();
        if (logs != null && !logs.isEmpty()) {
            // 重新计算总行数
            int totalLines = cachedLogLines.stream().mapToInt(LogLine::getLineCount).sum();
            if (totalLines > maxVisibleLines) {
                // 向上滚动（negative scroll = scroll up）
                scrollOffset = Math.max(0, scrollOffset - (int) verticalAmount);
                // 向下滚动
                int maxOffset = Math.max(0, totalLines - maxVisibleLines);
                scrollOffset = Math.min(maxOffset, scrollOffset + (int) verticalAmount);
                return true;
            }
        }
        return false;
    }

    /**
     * 重置滚动位置到底部
     */
    public void scrollToBottom() {
        List<String> logs = logSupplier.get();
        if (logs != null && !logs.isEmpty()) {
            int totalLines = cachedLogLines.stream().mapToInt(LogLine::getLineCount).sum();
            scrollOffset = Math.max(0, totalLines - maxVisibleLines);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // 无障碍：无需额外描述
    }
}
