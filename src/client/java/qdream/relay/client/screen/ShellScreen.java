package qdream.relay.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.renderer.RenderPipelines;

import qdream.relay.Relay;
import qdream.relay.screen.ShellScreenHandler;

/**
 * 外壳方块屏幕
 * 显示 4 个插槽：核心、法术磁盘、能量模块、世界交互器
 */
public class ShellScreen extends AbstractContainerScreen<ShellScreenHandler> {

    private static final Identifier CONTAINER_TEXTURE = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "textures/gui/container/shell.png");

    public ShellScreen(ShellScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        // 标题居中
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        // 玩家物品栏标题位置
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        // 渲染容器纹理背景
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CONTAINER_TEXTURE,
            this.leftPos,
            this.topPos,
            0.0F,
            0.0F,
            this.imageWidth,
            this.imageHeight,
            BACKGROUND_TEXTURE_WIDTH,
            BACKGROUND_TEXTURE_HEIGHT
        );
    }
}
