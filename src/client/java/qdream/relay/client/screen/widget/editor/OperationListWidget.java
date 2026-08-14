package qdream.relay.client.screen.widget.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import qdream.relay.client.screen.widget.editor.tools.InfoContent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 可用操作列表 Widget
 * 支持滚动浏览、点击选中、悬停高亮、搜索过滤
 * 点击某项操作时通过回调通知父级 Screen
 */
public class OperationListWidget extends AbstractWidget {

    private static final int HEADER_HEIGHT = 16;
    private static final int PADDING = 3;
    private static final int SEARCH_BOX_HEIGHT = 14;

    private static final int BG_COLOR = 0xFF1E1E1E;
    private static final int HEADER_BG = 0xFF252525;
    private static final int BORDER_COLOR = 0xFF3A3A3A;
    private static final int TEXT_COLOR = 0xFF00AA00;
    private static final int HOVER_COLOR = 0xFF55FF55;
    private static final int HOVER_BG = 0x3000FF00;
    private static final int SELECTED_BG = 0x4000AA00;
    private static final int SEARCH_BOX_BG = 0xFF2A2A2A;
    private static final int SEARCH_BOX_BORDER = 0xFF404040;

    private final ScrollableListWidget listWidget;
    private final Font font;
    private final List<InfoContent> operations;

    /** 搜索关键词 */
    private String searchText = "";

    /** 搜索框区域 */
    private int searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight;

    /** 光标位置（字符索引） */
    private int cursorPosition = 0;

    /** 光标聚焦开始时间（用于闪烁计算） */
    private long cursorFocusedTime = 0;

    public OperationListWidget(int x, int y, int width, int height, Font font, List<InfoContent> operations) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.operations = operations;

        // 搜索框区域
        this.searchBoxX = x + PADDING;
        this.searchBoxY = y + HEADER_HEIGHT + PADDING;
        this.searchBoxWidth = width - PADDING * 2;
        this.searchBoxHeight = SEARCH_BOX_HEIGHT;

        // 列表区域扣除标题栏和搜索框高度
        int listY = searchBoxY + searchBoxHeight + PADDING;
        int listHeight = height - HEADER_HEIGHT - searchBoxHeight - PADDING * 3;
        this.listWidget = new ScrollableListWidget(x, listY, width, listHeight, font, operations);
        this.listWidget.setColors(TEXT_COLOR, HOVER_COLOR, HOVER_BG, SELECTED_BG);
    }

    /**
     * 过滤操作列表
     */
    private void filterOperations() {
        if (searchText.isEmpty()) {
            listWidget.setDisplayItems(operations);
        } else {
            List<InfoContent> filtered = new ArrayList<>();
            for (InfoContent op : operations) {
                String name = op.name().text();
                String desc = op.description().text();
                String id = op.id();
                if (name.contains(searchText) || desc.contains(searchText) || id.contains(searchText)) {
                    filtered.add(op);
                }
            }
            listWidget.setDisplayItems(filtered);
        }
    }

    /**
     * 检查鼠标是否在搜索框内
     */
    public boolean isMouseOverSearchBox(double mouseX, double mouseY) {
        return mouseX >= searchBoxX && mouseX < searchBoxX + searchBoxWidth &&
                mouseY >= searchBoxY && mouseY < searchBoxY + searchBoxHeight;
    }

    /**
     * 检查光标是否可见（闪烁效果）
     * 使用绝对时间计算，避免帧率波动影响闪烁速度
     */
    private boolean isCursorVisible() {
        long elapsed = Util.getMillis() - this.cursorFocusedTime;
        return (elapsed / 500) % 2 == 0;
    }

    public void setOnOperationClicked(Consumer<String> callback) {
        this.listWidget.setOnItemSelected(callback);
    }

    public void setOnHover(Consumer<InfoContent> callback) {
        this.listWidget.setOnHover(callback);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 检查是否点击搜索框
        if (event.button() == 0 && isMouseOverSearchBox(event.x(), event.y())) {
            this.setFocused(true);
            this.cursorFocusedTime = Util.getMillis();
            // 设置光标位置为点击位置对应的字符索引
            this.cursorPosition = getCursorIndexAtX(event.x());
            return true;
        } else {
            this.setFocused(false);
        }
        return this.listWidget.mouseClicked(event, doubleClick);
    }

    /**
     * 根据鼠标 X 坐标计算光标位置
     */
    private int getCursorIndexAtX(double mouseX) {
        if (searchText.isEmpty()) {
            return 0;
        }
        int textX = searchBoxX + 4;
        String displayText = searchText;

        // 计算鼠标位置对应的字符索引
        int charIndex = 0;
        int currentWidth = 0;
        for (int i = 0; i < displayText.length(); i++) {
            int charWidth = font.width(String.valueOf(displayText.charAt(i)));
            if (textX + currentWidth + charWidth / 2 > mouseX) {
                break;
            }
            currentWidth += charWidth;
            charIndex++;
        }
        return Math.min(charIndex, displayText.length());
    }

    /**
     * 处理键盘按键事件
     */
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused()) {
            return false;
        }

        int key = event.key();

        // Backspace: 删除光标前的字符
        if (key == 259) { // GLFW_KEY_BACKSPACE
            if (cursorPosition > 0) {
                searchText = searchText.substring(0, cursorPosition - 1) + searchText.substring(cursorPosition);
                cursorPosition--;
                filterOperations();
            }
            return true;
        }

        // Delete: 删除光标后的字符
        if (key == 261) { // GLFW_KEY_DELETE
            if (cursorPosition < searchText.length()) {
                searchText = searchText.substring(0, cursorPosition) + searchText.substring(cursorPosition + 1);
                filterOperations();
            }
            return true;
        }

        // Left Arrow: 光标左移
        if (key == 263) { // GLFW_KEY_LEFT
            if (cursorPosition > 0) {
                cursorPosition--;
            }
            return true;
        }

        // Right Arrow: 光标右移
        if (key == 262) { // GLFW_KEY_RIGHT
            if (cursorPosition < searchText.length()) {
                cursorPosition++;
            }
            return true;
        }

        // Home: 光标移到开头
        if (key == 268) { // GLFW_KEY_HOME
            cursorPosition = 0;
            return true;
        }

        // End: 光标移到末尾
        if (key == 269) { // GLFW_KEY_END
            cursorPosition = searchText.length();
            return true;
        }

        // Escape: 取消聚焦并清空搜索
        if (key == 256) { // GLFW_KEY_ESCAPE
            this.setFocused(false);
            this.searchText = "";
            this.cursorPosition = 0;
            filterOperations();
            return true;
        }

        return false;
    }

    /**
     * 处理字符输入事件
     */
    public boolean charTyped(CharacterEvent event) {
        if (!isFocused()) {
            return false;
        }

        char c = event.codepointAsString().charAt(0);
        // 过滤控制字符（已经由 keyPressed 处理）
        if (!event.isAllowedChatCharacter()) {
            return false;
        }

        // 插入字符到光标位置
        searchText = searchText.substring(0, cursorPosition) + c + searchText.substring(cursorPosition);
        cursorPosition++;
        filterOperations();
        return true;
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
        graphics.text(this.font, Component.translatable("gui.relay:spell_editor.operations.title"),
                this.getX() + PADDING + 2, this.getY() + 4, 0xFF00FF00);

        // 搜索框背景
        graphics.fill(searchBoxX, searchBoxY, searchBoxX + searchBoxWidth, searchBoxY + searchBoxHeight, SEARCH_BOX_BG);

        // 聚焦时显示高亮边框
        int borderColor = isFocused() ? 0xFF6060FF : SEARCH_BOX_BORDER;
        graphics.outline(searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, borderColor);

        // 搜索提示文字或输入内容
        if (searchText.isEmpty()) {
            graphics.text(this.font, Component.translatable("gui.relay:spell_editor.operations.search"),
                    searchBoxX + 4, searchBoxY + 2, 0xFF808080);
        } else {
            graphics.text(this.font, searchText, searchBoxX + 4, searchBoxY + 2, TEXT_COLOR);

            if (isFocused()) {
                // 渲染光标（闪烁效果）-- 使用绝对时间计算，避免帧率波动影响
                if (isCursorVisible()) {
                    int cursorX = searchBoxX + 4;
                    if (cursorPosition > 0) {
                        // 计算光标 X 位置
                        String subText = searchText.substring(0, cursorPosition);
                        cursorX += font.width(subText);
                    }
                    graphics.verticalLine(cursorX, searchBoxY + 1, searchBoxY + searchBoxHeight - 2, 0xFFAAAA00);
                }
            }
        }

        // 渲染列表内容（由内部组件负责）
        this.listWidget.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        // 无障碍：无需额外描述
    }
}
