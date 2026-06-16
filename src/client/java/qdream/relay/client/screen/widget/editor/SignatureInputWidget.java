package qdream.relay.client.screen.widget.editor;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import qdream.relay.mc.signature.DataSignature;
import qdream.relay.mc.signature.Signature;
import qdream.relay.mc.signature.SignatureName;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.impl.locale.LikelySubtags.Data;

/**
 * 根据 Signature 动态创建输入框的 Widget
 * 支持滚动浏览、自动布局
 */
public class SignatureInputWidget extends AbstractWidget {

    private static final int FIELD_HEIGHT = 20;
    private static final int FIELD_SPACING = 6;
    private static final int PADDING = 8;
    private static final int LABEL_WIDTH = 60;
    private static final int SCROLLBAR_WIDTH = 4;

    private static final int BG_COLOR = 0xFF2A2A2A;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int LABEL_COLOR = 0xFFCCCCCC;
    private static final int SCROLLBAR_COLOR = 0xFF808080;
    private static final int SCROLLBAR_BG = 0xFF404040;

    private final Font font;
    private DataSignature signature;
    private final List<EditBox> inputFields = new ArrayList<>();
    private final List<String> fieldLabels = new ArrayList<>();

    /** 滚动偏移量（像素） */
    private int scrollOffset = 0;

    /** 内容总高度 */
    private int contentHeight = 0;

    /** 可视区域高度 */
    private int viewHeight = 0;

    public SignatureInputWidget(int x, int y, int width, DataSignature signature, Font font) {
        super(x, y, width, calculateViewHeight(width), Component.literal("Signature Input"));
        this.font = font;
        this.signature = signature;
        this.viewHeight = calculateViewHeight(width);
        if (signature != null) {
            createInputFields();
        }
    }

    private static int calculateViewHeight(int width) {
        // 最小高度100，最大200
        return Math.min(200, Math.max(100, width / 2));
    }

    private void createInputFields() {
        inputFields.clear();
        fieldLabels.clear();

        if (signature == null)
            return;

        int inputCount = signature.inputCount();
        contentHeight = inputCount * (FIELD_HEIGHT + FIELD_SPACING) + PADDING * 2;

        for (int i = 0; i < inputCount; i++) {
            SignatureName signatureName = signature.getInputs().get(i);
            String hintText = buildHintText(signatureName);
            String label = "P" + (i + 1);

            fieldLabels.add(label);

            EditBox editBox = new EditBox(
                    font,
                    getX() + PADDING + LABEL_WIDTH,
                    getY() + PADDING + i * (FIELD_HEIGHT + FIELD_SPACING) - scrollOffset,
                    getFieldWidth(),
                    FIELD_HEIGHT,
                    Component.literal(""));

            editBox.setHint(Component.literal(hintText).withStyle(ChatFormatting.GRAY));
            editBox.setMaxLength(256);
            editBox.setBordered(true);

            inputFields.add(editBox);
        }
    }

    private String buildHintText(SignatureName types) {
        if (types == null) {
            return "any";
        }
        if (types.getType().size() == 1) {
            return types.getName() + ":" + types.getType().get(0);
        }
        return types.getName() + ":" + String.join(" | ", types.getType());
    }

    private int getFieldWidth() {
        return this.width - PADDING * 2 - LABEL_WIDTH - SCROLLBAR_WIDTH;
    }

    private int getVisibleFieldCount() {
        return Math.max(1, (viewHeight - PADDING * 2) / (FIELD_HEIGHT + FIELD_SPACING));
    }

    private int getMaxScroll() {
        if (inputFields.isEmpty())
            return 0;
        int totalHeight = inputFields.size() * (FIELD_HEIGHT + FIELD_SPACING);
        int visibleHeight = viewHeight - PADDING * 2;
        return Math.max(0, totalHeight - visibleHeight);
    }

    private void updateFieldPositions() {
        for (int i = 0; i < inputFields.size(); i++) {
            EditBox field = inputFields.get(i);
            int fieldY = getY() + PADDING + i * (FIELD_HEIGHT + FIELD_SPACING) - scrollOffset;
            field.setX(getX() + PADDING + LABEL_WIDTH);
            field.setY(fieldY);
        }
    }

    /**
     * 更新签名并重建输入框
     */
    public void updateSignature(DataSignature newSignature) {
        this.signature = newSignature;
        this.scrollOffset = 0;

        // 清空旧输入框
        inputFields.clear();
        fieldLabels.clear();

        if (signature != null) {
            createInputFields();
        }

        // 重新计算高度
        this.viewHeight = calculateViewHeight(this.width);
        this.height = viewHeight;
    }

    /**
     * 更新位置
     */
    public void updatePosition(int x, int y) {
        this.setX(x);
        this.setY(y);
        updateFieldPositions();
    }

    /**
     * 获取所有输入值
     */
    public List<String> getInputValues() {
        List<String> values = new ArrayList<>();
        for (EditBox field : inputFields) {
            values.add(field.getValue());
        }
        return values;
    }

    /**
     * 设置指定索引的值
     */
    public void setValue(int index, String value) {
        if (index >= 0 && index < inputFields.size()) {
            inputFields.get(index).setValue(value);
        }
    }

    /**
     * 清空所有输入
     */
    public void clear() {
        for (EditBox field : inputFields) {
            field.setValue("");
        }
    }

    /**
     * 聚焦第一个输入框
     */
    public void focusFirst() {
        if (!inputFields.isEmpty()) {
            inputFields.get(0).setFocused(true);
        }
    }

    /**
     * 获取输入框数量
     */
    public int getFieldCount() {
        return inputFields.size();
    }

    // ==================== 事件处理 ====================
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.visible)
            return false;

        // 检查是否点击在滚动条区域
        int scrollbarX = getX() + this.width - SCROLLBAR_WIDTH;
        if (event.x() >= scrollbarX && event.x() <= scrollbarX + SCROLLBAR_WIDTH) {
            handleScrollbarClick(event.y());
            return true;
        }

        // 转发给输入框，并管理焦点
        for (EditBox field : inputFields) {
            if (isMouseOverField(field, event.x(), event.y())) {
                field.setFocused(true);
                return field.mouseClicked(event, doubleClick);
            }
        }

        // 点击在空白区域，取消所有焦点
        for (EditBox field : inputFields) {
            field.setFocused(false);
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // 转发给当前聚焦的输入框
        for (EditBox field : inputFields) {
            if (field.isFocused()) {
                return field.keyPressed(event);
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        // 转发给当前聚焦的输入框
        for (EditBox field : inputFields) {
            if (field.isFocused()) {
                return field.charTyped(event);
            }
        }
        return super.charTyped(event);
    }

    private boolean isMouseOverField(EditBox field, double mouseX, double mouseY) {
        return mouseX >= field.getX() && mouseX <= field.getX() + field.getWidth()
                && mouseY >= field.getY() && mouseY <= field.getY() + field.getHeight();
    }

    private void handleScrollbarClick(double mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0)
            return;

        float ratio = (float) (mouseY - getY() - PADDING) / (viewHeight - PADDING * 2);
        scrollOffset = (int) (maxScroll * Math.max(0, Math.min(1, ratio)));
        updateFieldPositions();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        // 滚动条拖动
        if (this.visible) {
            int scrollbarX = getX() + this.width - SCROLLBAR_WIDTH;
            if (event.x() >= scrollbarX && event.x() <= scrollbarX + SCROLLBAR_WIDTH) {
                handleScrollbarClick(event.y());
                return true;
            }
        }

        // 转发给输入框
        for (EditBox field : inputFields) {
            if (isMouseOverField(field, event.x(), event.y())) {
                return field.mouseDragged(event, dx, dy);
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.visible || inputFields.isEmpty())
            return false;

        int maxScroll = getMaxScroll();
        if (maxScroll <= 0)
            return false;

        scrollOffset -= (int) scrollY * (FIELD_HEIGHT + FIELD_SPACING);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        updateFieldPositions();
        return true;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            for (EditBox field : inputFields) {
                field.setFocused(false);
            }
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && mouseX >= getX() && mouseX <= getX() + width
                && mouseY >= getY() && mouseY <= getY() + height;
    }

    // ==================== 渲染 ====================

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!this.visible || inputFields.isEmpty())
            return;

        int x = getX();
        int y = getY();

        // 背景
        graphics.fill(x, y, x + this.width, y + this.height, BG_COLOR);
        graphics.outline(x, y, this.width, this.height, BORDER_COLOR);

        // 启用裁剪
        int clipX = x + PADDING;
        int clipY = y + PADDING;
        int clipWidth = this.width - PADDING * 2;
        int clipHeight = this.height - PADDING * 2;
        graphics.enableScissor(clipX, clipY, clipX + clipWidth, clipY + clipHeight);

        // 渲染输入框
        for (EditBox field : inputFields) {
            if (field.visible) {
                field.extractWidgetRenderState(graphics, mouseX, mouseY, delta);
            }
        }

        // 渲染标签
        int labelX = x + PADDING + 2;
        int startIndex = getStartIndex();
        int visibleCount = getVisibleFieldCount();

        for (int i = 0; i < visibleCount && (startIndex + i) < fieldLabels.size(); i++) {
            int idx = startIndex + i;
            String label = fieldLabels.get(idx);
            int labelY = y + PADDING + i * (FIELD_HEIGHT + FIELD_SPACING) -
                    (startIndex > 0 ? getOffsetForIndex(startIndex) : scrollOffset);

            // 只渲染在可见区域内的标签
            if (labelY >= y + PADDING && labelY + FIELD_HEIGHT <= y + this.height - PADDING) {
                graphics.text(font, label + ":", labelX, labelY + (FIELD_HEIGHT - 8) / 2, LABEL_COLOR);
            }
        }

        graphics.disableScissor();

        // 渲染滚动条（如果需要）
        if (getMaxScroll() > 0) {
            renderScrollBar(graphics, x, y);
        }

        // 无参数提示
        if (inputFields.isEmpty()) {
            String msg = "无参数";
            int msgX = x + (this.width - font.width(msg)) / 2;
            int msgY = y + (this.height - 8) / 2;
            graphics.text(font, msg, msgX, msgY, 0xFF666666);
        }
    }

    private int getStartIndex() {
        if (inputFields.isEmpty())
            return 0;
        int perField = FIELD_HEIGHT + FIELD_SPACING;
        if (perField == 0)
            return 0;
        return Math.max(0, Math.min(inputFields.size() - 1, scrollOffset / perField));
    }

    private int getOffsetForIndex(int index) {
        return index * (FIELD_HEIGHT + FIELD_SPACING);
    }

    private void renderScrollBar(GuiGraphicsExtractor graphics, int x, int y) {
        int sbX = x + this.width - SCROLLBAR_WIDTH - 2;
        int sbTop = y + PADDING;
        int sbHeight = this.height - PADDING * 2;

        // 滚动条背景
        graphics.fill(sbX, sbTop, sbX + SCROLLBAR_WIDTH, sbTop + sbHeight, SCROLLBAR_BG);

        // 滑块大小
        int visibleCount = getVisibleFieldCount();
        int totalCount = inputFields.size();
        float ratio = (float) visibleCount / totalCount;
        int thumbHeight = Math.max(20, (int) (sbHeight * ratio));

        // 滑块位置
        float scrollRatio = getMaxScroll() > 0 ? (float) scrollOffset / getMaxScroll() : 0;
        int thumbY = sbTop + (int) ((sbHeight - thumbHeight) * scrollRatio);

        graphics.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + thumbHeight, SCROLLBAR_COLOR);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // 可选实现
    }
}