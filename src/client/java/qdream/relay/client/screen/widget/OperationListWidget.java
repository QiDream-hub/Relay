package qdream.relay.client.screen.widget;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 操作列表 Widget
 */
public class OperationListWidget extends AbstractWidget {
    private java.util.function.Consumer<String> onSelect;

    public OperationListWidget(int x, int y, int width, int height, java.util.function.Consumer<String> onSelect) {
        super(x, y, width, height, Component.literal("Operations"));
        this.onSelect = onSelect;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // 简单实现
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
