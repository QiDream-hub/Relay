package qdream.relay.client.renderer;

import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;

/**
 * StringDisplay 实体渲染器
 */
public class StringDisplayRenderer extends DisplayRenderer.TextDisplayRenderer {

    public StringDisplayRenderer(Context context) {
        super(context);
    }

    @Override
    public TextDisplayEntityRenderState createRenderState() {
        return new TextDisplayEntityRenderState();
    }
}
