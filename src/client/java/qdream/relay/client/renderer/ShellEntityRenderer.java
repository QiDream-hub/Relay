package qdream.relay.client.renderer;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import qdream.relay.entities.EntityShell;

/**
 * Shell 实体渲染器
 * 仅显示粒子效果，无实体模型
 */
public class ShellEntityRenderer extends EntityRenderer<EntityShell, EntityRenderState> {

    public ShellEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public boolean shouldRender(EntityShell entity, Frustum frustum, double camX, double camY, double camZ) {
        // 始终渲染（用于粒子效果）
        return true;
    }
}
