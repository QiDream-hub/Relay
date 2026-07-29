package qdream.relay.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import qdream.relay.client.renderer.ShellEntityRenderer;
import qdream.relay.entities.RelayEntities;

/**
 * 客户端渲染器注册
 * 注意：26.1.2 API 变化较大，实体渲染器暂时简化实现
 */
public class RelayEntityRenderers {

    public static void register() {
        // 注册 Shell 实体渲染器（粒子效果）
        EntityRendererRegistry.register(RelayEntities.ENTITY_SHELL, ShellEntityRenderer::new);
    }
}
