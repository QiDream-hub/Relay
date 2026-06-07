package qdream.relay.client;

import net.minecraft.client.gui.screens.MenuScreens;

import qdream.relay.client.screen.ShellScreen;
import qdream.relay.client.editor.SpellEditorScreen;
import qdream.relay.screen.RelayScreenHandlers;

/**
 * 客户端 Screen 注册表
 * 在客户端初始化时注册所有 Screen
 */
public class RelayScreenHandlersClient {

    public static void init() {
        // 注册外壳方块屏幕
        MenuScreens.register(RelayScreenHandlers.SHELL_SCREEN_HANDLER, ShellScreen::new);

        // 注册法术编辑器屏幕
        MenuScreens.register(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, SpellEditorScreen::new);
    }
}
