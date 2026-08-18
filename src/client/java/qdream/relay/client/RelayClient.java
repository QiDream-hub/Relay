package qdream.relay.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * 客户端入口
 */
public class RelayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册 Screen 和实体渲染器
        RelayScreenHandlersClient.init();
        RelayEntityRenderers.register();

        // 注册按键绑定
        RelayKeyBindings.register();

        // 注册网络处理
        qdream.relay.client.networking.RelayClientNetworking.register();
        
        // 注册键盘监听器
        ClientKeyboardListener.register();
    }
}
