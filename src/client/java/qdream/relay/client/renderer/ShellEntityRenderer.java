package qdream.relay.client.renderer;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import qdream.relay.entities.EntityShell;

/**
 * Shell 实体渲染器
 * 无实体模型，仅渲染粒子效果（粒子在 Entity.tick() 中生成）
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
    public void extractRenderState(EntityShell entity, EntityRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
    }

    @Override
    public boolean shouldRender(EntityShell entity, Frustum frustum, double camX, double camY, double camZ) {
        // 始终渲染以确保粒子效果可见
        return true;
    }
}
