package qdream.relay.client.screen.widget.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * 自定义多行文本编辑器
 * 支持精确的光标位置控制、选区、插入等操作
 * 简化版本，不依赖 Minecraft 内部类
 */
public class CustomMultiLineEditBox extends AbstractTextAreaWidget {

    private static final int CURSOR_COLOR = -3092272;
    private static final int PLACEHOLDER_TEXT_COLOR = ARGB.color(204, -2039584);
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int SELECTION_COLOR = 0x800080FF;
    private static final int LINE_HEIGHT = 9;

    private final Font font;
    private final Component placeholder;
    private final int textColor;
    private final boolean textShadow;
    private final int cursorColor;

    /** 文本内容 */
    private StringBuilder text = new StringBuilder();

    /** 光标位置（字符索引） */
    private int cursorPos = 0;

    /** 选区起始位置（-1 表示无选区） */
    private int selectionStart = -1;

    /** 聚焦时间（用于光标闪烁） */
    private long focusedTime = Util.getMillis();

    /** 滚动偏移量（像素） */
    private double scrollOffset = 0;

    /** 文本变化监听器 */
    @Nullable
    private Runnable valueListener;

    public CustomMultiLineEditBox(
        final Font font,
        final int x,
        final int y,
        final int width,
        final int height,
        final Component placeholder,
        final int textColor,
        final boolean textShadow,
        final int cursorColor,
        final boolean showBackground,
        final boolean showDecorations
    ) {
        super(x, y, width, height, Component.empty(), defaultSettings(4), showBackground, showDecorations);
        this.font = font;
        this.placeholder = placeholder;
        this.textColor = textColor;
        this.textShadow = textShadow;
        this.cursorColor = cursorColor;
    }

    /**
     * 设置文本变化监听器
     */
    public void setValueListener(Runnable listener) {
        this.valueListener = listener;
    }

    /**
     * 获取当前文本
     */
    public String getValue() {
        return this.text.toString();
    }

    /**
     * 设置文本内容
     */
    public void setValue(String value) {
        this.text = new StringBuilder(value);
        this.cursorPos = Math.min(this.cursorPos, this.text.length());
        this.ensureCursorVisible();
        this.onValueChanged();
    }

    /**
     * 获取光标位置
     */
    public int getCursorPos() {
        return this.cursorPos;
    }

    /**
     * 设置光标位置
     */
    public void setCursorPos(int pos) {
        this.cursorPos = Math.max(0, Math.min(pos, this.text.length()));
        this.selectionStart = -1;
        this.ensureCursorVisible();
    }

    /**
     * 在光标位置插入文本
     */
    public void insertText(String textToInsert) {
        if (hasSelection()) {
            // 有选区时替换选区
            int start = Math.min(this.cursorPos, this.selectionStart);
            int end = Math.max(this.cursorPos, this.selectionStart);
            this.text.replace(start, end, textToInsert);
            this.cursorPos = start + textToInsert.length();
            this.selectionStart = -1;
        } else {
            // 无选区时插入
            this.text.insert(this.cursorPos, textToInsert);
            this.cursorPos += textToInsert.length();
        }
        this.ensureCursorVisible();
        this.onValueChanged();
    }

    /**
     * 在光标位置插入文本并自动追加逗号
     * 插入后光标位于逗号之后，不选中任何内容
     * @param textToInsert 要插入的文本（不含逗号）
     */
    public void insertWithComma(String textToInsert) {
        String fullText = textToInsert + ",";
        insertText(fullText);
    }

    /**
     * 删除光标前的字符
     */
    public void deleteBackward() {
        if (hasSelection()) {
            // 删除选区
            int start = Math.min(this.cursorPos, this.selectionStart);
            int end = Math.max(this.cursorPos, this.selectionStart);
            this.text.replace(start, end, "");
            this.cursorPos = start;
            this.selectionStart = -1;
        } else if (this.cursorPos > 0) {
            this.text.deleteCharAt(this.cursorPos - 1);
            this.cursorPos--;
        }
        this.ensureCursorVisible();
        this.onValueChanged();
    }

    /**
     * 删除光标后的字符
     */
    public void deleteForward() {
        if (hasSelection()) {
            int start = Math.min(this.cursorPos, this.selectionStart);
            int end = Math.max(this.cursorPos, this.selectionStart);
            this.text.replace(start, end, "");
            this.cursorPos = start;
            this.selectionStart = -1;
        } else if (this.cursorPos < this.text.length()) {
            this.text.deleteCharAt(this.cursorPos);
        }
        this.ensureCursorVisible();
        this.onValueChanged();
    }

    /**
     * 是否有选区
     */
    public boolean hasSelection() {
        return this.selectionStart >= 0 && this.selectionStart != this.cursorPos;
    }

    /**
     * 获取选区起始位置
     */
    public int getSelectionStart() {
        return Math.min(this.cursorPos, this.selectionStart);
    }

    /**
     * 获取选区结束位置
     */
    public int getSelectionEnd() {
        return Math.max(this.cursorPos, this.selectionStart);
    }

    /**
     * 清除选区
     */
    public void clearSelection() {
        this.selectionStart = -1;
    }

    /**
     * 设置选区
     */
    public void setSelection(int start, int end) {
        this.selectionStart = start;
        this.cursorPos = end;
    }

    /**
     * 获取总行数
     */
    public int getLineCount() {
        if (this.text.length() == 0) {
            return 1;
        }
        int count = 1;
        for (int i = 0; i < this.text.length(); i++) {
            if (this.text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取内容高度
     */
    @Override
    protected int getInnerHeight() {
        return getLineCount() * LINE_HEIGHT;
    }

    /**
     * 确保光标在可视区域内
     */
    private void ensureCursorVisible() {
        int cursorLine = getCurrentLine();
        int cursorY = cursorLine * LINE_HEIGHT;

        double scroll = this.scrollOffset;
        int visibleHeight = this.height - this.totalInnerPadding();

        if (cursorY < scroll) {
            this.scrollOffset = cursorY;
        } else if (cursorY + LINE_HEIGHT > scroll + visibleHeight) {
            this.scrollOffset = cursorY + LINE_HEIGHT - visibleHeight;
        }
    }

    /**
     * 获取光标所在行号
     */
    private int getCurrentLine() {
        int line = 0;
        for (int i = 0; i < this.cursorPos && i < this.text.length(); i++) {
            if (this.text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    /**
     * 根据屏幕坐标计算光标位置
     */
    private int calculateCursorPos(double mouseX, double mouseY) {
        double relX = mouseX - this.getInnerLeft();
        double relY = mouseY - this.getInnerTop() + this.scrollOffset;

        int targetLine = (int) (relY / LINE_HEIGHT);
        targetLine = Math.max(0, Math.min(targetLine, getLineCount() - 1));

        // 找到目标行的起始和结束位置
        int lineStart = 0;
        int currentLine = 0;
        for (int i = 0; i < this.text.length(); i++) {
            if (currentLine == targetLine) {
                break;
            }
            if (this.text.charAt(i) == '\n') {
                lineStart = i + 1;
                currentLine++;
            }
        }

        int lineEnd = lineStart;
        while (lineEnd < this.text.length() && this.text.charAt(lineEnd) != '\n') {
            lineEnd++;
        }

        // 计算在该行中的字符位置
        String lineText = this.text.substring(lineStart, lineEnd);
        int charIndex = 0;
        int accumulatedWidth = 0;
        for (int i = 0; i < lineText.length(); i++) {
            int charWidth = this.font.width(String.valueOf(lineText.charAt(i)));
            if (accumulatedWidth + charWidth / 2 > relX) {
                break;
            }
            accumulatedWidth += charWidth;
            charIndex++;
        }

        return lineStart + charIndex;
    }

    /**
     * 文本变化回调
     */
    private void onValueChanged() {
        if (this.valueListener != null) {
            this.valueListener.run();
        }
    }

    @Override
    public void setScrollAmount(double amount) {
        this.scrollOffset = amount;
    }

    @Override
    public double scrollAmount() {
        return this.scrollOffset;
    }

    @Override
    public double scrollRate() {
        return LINE_HEIGHT;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.visible && this.isMouseOver(event.x(), event.y())) {
            this.setFocused(true);
            this.focusedTime = Util.getMillis();

            int newPos = calculateCursorPos(event.x(), event.y());

            if (doubleClick) {
                // 双击选中单词
                this.cursorPos = newPos;
                selectWordAtCursor();
            } else if (event.hasShiftDown() && this.selectionStart >= 0) {
                // 按住 Shift 扩展选区
                this.cursorPos = newPos;
            } else {
                // 单击：仅移动光标，不创建选区
                this.cursorPos = newPos;
                this.selectionStart = -1;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (this.isFocused() && event.button() == 0) {
            int newPos = calculateCursorPos(event.x(), event.y());
            // 拖动时保持选区起始点不变，只更新光标位置
            if (this.selectionStart < 0) {
                this.selectionStart = this.cursorPos;
            }
            this.cursorPos = newPos;
            return true;
        }
        return false;
    }

    /**
     * 选中光标处的单词
     */
    private void selectWordAtCursor() {
        int start = this.cursorPos;
        int end = this.cursorPos;

        // 向前查找单词边界
        while (start > 0 && !isWhitespace(this.text.charAt(start - 1))) {
            start--;
        }

        // 向后查找单词边界
        while (end < this.text.length() && !isWhitespace(this.text.charAt(end))) {
            end++;
        }

        this.selectionStart = start;
        this.cursorPos = end;
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\t' || c == '\r';
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!this.isFocused()) {
            return false;
        }

        // 方向键移动光标
        if (event.isDown()) {
            moveCursorDown();
            return true;
        }
        if (event.isUp()) {
            moveCursorUp();
            return true;
        }
        if (event.isLeft()) {
            moveCursorLeft(event.hasShiftDown());
            return true;
        }
        if (event.isRight()) {
            moveCursorRight(event.hasShiftDown());
            return true;
        }

        // Home/End 键
        if (event.key() == 268) { // HOME
            moveToLineStart(event.hasShiftDown());
            return true;
        }
        if (event.key() == 269) { // END
            moveToLineEnd(event.hasShiftDown());
            return true;
        }

        // 删除键
        if (event.key() == 259) { // BACKSPACE
            deleteBackward();
            return true;
        }
        if (event.key() == 261) { // DELETE
            deleteForward();
            return true;
        }

        // Enter 键
        if (event.key() == 257) {
            insertText("\n");
            return true;
        }

        // Ctrl+A 全选
        if (event.hasControlDown() && event.key() == 65) {
            this.selectionStart = 0;
            this.cursorPos = this.text.length();
            return true;
        }

        // Ctrl+C 复制
        if (event.hasControlDown() && event.key() == 67 && hasSelection()) {
            String selected = this.text.substring(getSelectionStart(), getSelectionEnd());
            Minecraft.getInstance().keyboardHandler.setClipboard(selected);
            return true;
        }

        // Ctrl+V 粘贴
        if (event.hasControlDown() && event.key() == 86) {
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                insertText(clipboard);
            }
            return true;
        }

        // Ctrl+X 剪切
        if (event.hasControlDown() && event.key() == 88 && hasSelection()) {
            String selected = this.text.substring(getSelectionStart(), getSelectionEnd());
            Minecraft.getInstance().keyboardHandler.setClipboard(selected);
            deleteBackward();
            return true;
        }

        return false;
    }

    private void moveCursorLeft(boolean shift) {
        if (this.cursorPos > 0) {
            if (!shift) {
                this.selectionStart = -1;
            }
            this.cursorPos--;
            this.ensureCursorVisible();
        }
    }

    private void moveCursorRight(boolean shift) {
        if (this.cursorPos < this.text.length()) {
            if (!shift) {
                this.selectionStart = -1;
            }
            this.cursorPos++;
            this.ensureCursorVisible();
        }
    }

    private void moveCursorUp() {
        int currentLine = getCurrentLine();
        if (currentLine > 0) {
            // 找到上一行的起始位置
            int prevLineStart = getLineStart(currentLine - 1);
            // 保持在行内的相同列位置
            int col = this.cursorPos - getLineStart(currentLine);
            int prevLineEnd = getLineEnd(currentLine - 1);
            this.cursorPos = Math.min(prevLineStart + col, prevLineEnd);
            if (!this.hasSelection()) {
                this.selectionStart = -1;
            }
            this.ensureCursorVisible();
        }
    }

    private void moveCursorDown() {
        int currentLine = getCurrentLine();
        int totalLines = getLineCount();
        if (currentLine < totalLines - 1) {
            int nextLineStart = getLineStart(currentLine + 1);
            int col = this.cursorPos - getLineStart(currentLine);
            this.cursorPos = Math.min(nextLineStart + col, getLineEnd(currentLine + 1));
            if (!this.hasSelection()) {
                this.selectionStart = -1;
            }
            this.ensureCursorVisible();
        }
    }

    private int getLineStart(int line) {
        int currentLine = 0;
        int pos = 0;
        while (currentLine < line && pos < this.text.length()) {
            if (this.text.charAt(pos) == '\n') {
                currentLine++;
            }
            pos++;
        }
        return currentLine == line ? pos : this.text.length();
    }

    private int getLineEnd(int line) {
        int start = getLineStart(line);
        while (start < this.text.length() && this.text.charAt(start) != '\n') {
            start++;
        }
        return start;
    }

    private int getCurrentLineAt(int pos) {
        int line = 0;
        for (int i = 0; i < pos && i < this.text.length(); i++) {
            if (this.text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private void moveToLineStart(boolean shift) {
        int currentLine = getCurrentLine();
        int lineStart = getLineStart(currentLine);
        if (shift) {
            this.cursorPos = lineStart;
        } else {
            this.selectionStart = -1;
            this.cursorPos = lineStart;
        }
    }

    private void moveToLineEnd(boolean shift) {
        int currentLine = getCurrentLine();
        int lineEnd = getLineEnd(currentLine);
        if (shift) {
            this.cursorPos = lineEnd;
        } else {
            this.selectionStart = -1;
            this.cursorPos = lineEnd;
        }
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.isFocused() && event.isAllowedChatCharacter()) {
            // 使用 Character.toString() 将 codepoint 转换为字符串
            insertText(Character.toString(event.codepoint()));
            return true;
        }
        return false;
    }

    public boolean preeditUpdated(@Nullable Object event) {
        // 简化版本不支持 IME
        return true;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        String value = this.text.toString();
        boolean showCursor = this.isFocused() && isCursorVisible();

        int drawTop = this.getInnerTop();
        int innerLeft = this.getInnerLeft();
        boolean hasDrawnCursor = false;

        // 按行渲染
        int lineStart = 0;
        int lineEnd = 0;

        while (lineStart <= value.length()) {
            lineEnd = value.indexOf('\n', lineStart);
            if (lineEnd == -1) {
                lineEnd = value.length();
            }

            boolean lineVisible = this.withinContentAreaTopBottom(drawTop, drawTop + LINE_HEIGHT);

            if (lineVisible) {
                // 渲染选区高亮
                if (hasSelection()) {
                    int selStart = getSelectionStart();
                    int selEnd = getSelectionEnd();
                    if (selStart < lineEnd && selEnd > lineStart) {
                        int highlightStart = Math.max(selStart, lineStart);
                        int highlightEnd = Math.min(selEnd, lineEnd);
                        int highlightX = innerLeft + this.font.width(value.substring(lineStart, highlightStart));
                        int highlightWidth = this.font.width(value.substring(highlightStart, highlightEnd));
                        graphics.fill(highlightX, drawTop, highlightX + highlightWidth, drawTop + LINE_HEIGHT, SELECTION_COLOR);
                    }
                }

                // 渲染光标
                if (showCursor && !hasDrawnCursor && this.cursorPos >= lineStart && this.cursorPos <= lineEnd) {
                    String textBeforeCursor = value.substring(lineStart, this.cursorPos);
                    int cursorX = innerLeft + this.font.width(textBeforeCursor);
                    // 绘制光标（简单的竖线）
                    graphics.fill(cursorX, drawTop, cursorX + 1, drawTop + LINE_HEIGHT, this.cursorColor);
                    hasDrawnCursor = true;
                }

                // 渲染文本
                String lineText = value.substring(lineStart, lineEnd);
                graphics.text(this.font, lineText, innerLeft, drawTop, this.textColor, this.textShadow);
            }

            drawTop += LINE_HEIGHT;
            lineStart = lineEnd + 1;
        }

        // 渲染光标在末尾
        if (showCursor && !hasDrawnCursor && this.cursorPos == value.length()) {
            int lastLineY = this.getInnerTop() + (getLineCount() - 1) * LINE_HEIGHT;
            int lastLineStart = getLineStart(getLineCount() - 1);
            int lastLineWidth = this.font.width(value.substring(lastLineStart));
            // 绘制光标（简单的竖线）
            graphics.fill(innerLeft + lastLineWidth, lastLineY, innerLeft + lastLineWidth + 1, lastLineY + LINE_HEIGHT, this.cursorColor);
        }
    }

    /**
     * 检查光标是否可见（闪烁效果）
     */
    private boolean isCursorVisible() {
        long elapsed = Util.getMillis() - this.focusedTime;
        return (elapsed / 300) % 2 == 0;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            this.focusedTime = Util.getMillis();
        }
        Minecraft.getInstance().onTextInputFocusChange(this, focused);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
    }
}
