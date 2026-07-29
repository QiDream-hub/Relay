package qdream.relay.client.screen.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * 插槽 Widget - 简单的方框表示物品插槽位置
 * 用于编辑器和各个 Shell 界面显示可以放置物品的插槽
 */
public class SlotWidget extends AbstractWidget {

    // 插槽尺寸
    private static final int SLOT_SIZE = 18;

    // 颜色
    private static final int BG_COLOR = 0xFF3A3A3A;
    private static final int BORDER_COLOR = 0xFF8B8B8B;

    /**
     * 创建插槽 Widget
     *
     * @param x    X 坐标（相对于 GUI 左上角）
     * @param y    Y 坐标（相对于 GUI 左上角）
     * @param name 插槽名称
     */
    public SlotWidget(int x, int y, String name) {
        super(x - 1, y - 1, SLOT_SIZE, SLOT_SIZE, Component.empty());
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // 背景
        graphics.fill(this.getX(), this.getY(), this.getX() + SLOT_SIZE, this.getY() + SLOT_SIZE, BG_COLOR);

        // 边框（始终使用普通边框颜色，不响应悬停）
        graphics.outline(this.getX(), this.getY(), SLOT_SIZE, SLOT_SIZE, BORDER_COLOR);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // 始终返回 false，让事件传递给下层的物品栏 Widget
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 不拦截点击事件，让事件传递给下层的物品栏 Widget
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        // 不拦截拖拽事件，让事件传递给下层的物品栏 Widget
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // 不拦截释放事件，让事件传递给下层的物品栏 Widget
        return false;
    }

    /**
     * 获取标准插槽尺寸
     */
    public static int getSlotSize() {
        return SLOT_SIZE;
    }
}
