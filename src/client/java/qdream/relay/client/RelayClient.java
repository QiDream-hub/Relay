package qdream.relay.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

import qdream.relay.client.networking.RelayClientNetworking;
import qdream.relay.client.editor.SpellEditorScreen;
import qdream.relay.screen.RelayScreenHandlers;

/**
 * 客户端入口
 */
public class RelayClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册 Screen 工厂
        MenuScreens.register(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, SpellEditorScreen::new);

        // 注册实体渲染器
        RelayEntityRenderers.register();

        // 注册网络处理
        RelayClientNetworking.register();
    }
}
