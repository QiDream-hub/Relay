package qdream.relay.client.screen.widget;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 程序编辑器 Widget
 */
public class ProgramEditorWidget extends AbstractWidget {
    public ProgramEditorWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Program Editor"));
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // 简单实现
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
