package qdream.relay.client;

import net.minecraft.client.renderer.entity.EntityRenderers;
import qdream.relay.client.renderer.ShellEntityRenderer;
import qdream.relay.entities.RelayEntities;

/**
 * 客户端渲染器注册
 * 26.1.2 使用 EntityRenderers.register() 替代 Fabric 的 EntityRendererRegistry
 */
public class RelayEntityRenderers {

    public static void register() {
        // 注册 Shell 实体渲染器（粒子效果）
        EntityRenderers.register(RelayEntities.ENTITY_SHELL, ShellEntityRenderer::new);
    }
}
