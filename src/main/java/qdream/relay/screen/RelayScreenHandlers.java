package qdream.relay.screen;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;

import qdream.relay.Relay;

/**
 * ScreenHandler 注册表
 */
public class RelayScreenHandlers {

    public static MenuType<ShellScreenHandler> SHELL_SCREEN_HANDLER;
    public static MenuType<SpellEditorScreenHandler> SPELL_EDITOR_SCREEN_HANDLER;
    public static MenuType<ToolShellScreenHandler> TOOL_SHELL_SCREEN_HANDLER;

    private static boolean initialized = false;

    /**
     * 初始化并注册所有 MenuType
     * 必须在游戏初始化阶段调用，确保注册表未冻结
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        SHELL_SCREEN_HANDLER = new MenuType<>((syncId, inventory) -> new ShellScreenHandler(syncId, inventory), FeatureFlags.VANILLA_SET);
        Identifier id = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "shell");
        Registry.register(BuiltInRegistries.MENU, id, SHELL_SCREEN_HANDLER);

        SPELL_EDITOR_SCREEN_HANDLER = new MenuType<>((syncId, inventory) -> new SpellEditorScreenHandler(syncId, inventory), FeatureFlags.VANILLA_SET);
        Identifier editorId = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "spell_editor");
        Registry.register(BuiltInRegistries.MENU, editorId, SPELL_EDITOR_SCREEN_HANDLER);

        TOOL_SHELL_SCREEN_HANDLER = new MenuType<>((syncId, inventory) -> new ToolShellScreenHandler(syncId, inventory), FeatureFlags.VANILLA_SET);
        Identifier toolId = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "tool_shell");
        Registry.register(BuiltInRegistries.MENU, toolId, TOOL_SHELL_SCREEN_HANDLER);
    }
}
