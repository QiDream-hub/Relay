package qdream.relay.client;

import net.fabricmc.api.ClientModInitializer;

import qdream.relay.client.networking.RelayClientNetworking;
import qdream.relay.screen.RelayScreenHandlers;

/**
 * 客户端入口
 */
public class RelayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册 ScreenHandler
        RelayScreenHandlers.init();

        // 注册实体渲染器
        RelayEntityRenderers.register();

        // 注册网络处理
        RelayClientNetworking.register();
    }
}
