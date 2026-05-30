package qdream.relay.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * 客户端入口
 */
public class RelayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册 ScreenHandler
        RelayScreenHandlers.init();
    }
}
