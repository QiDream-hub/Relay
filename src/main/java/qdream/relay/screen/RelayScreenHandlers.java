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

    public static final MenuType<ShellScreenHandler> SHELL_SCREEN_HANDLER;
    public static final MenuType<SpellEditorScreenHandler> SPELL_EDITOR_SCREEN_HANDLER;

    static {
        SHELL_SCREEN_HANDLER = new MenuType<>((syncId, inventory) -> new ShellScreenHandler(syncId, inventory), FeatureFlags.VANILLA_SET);
        Identifier id = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "shell");
        Registry.register(BuiltInRegistries.MENU, id, SHELL_SCREEN_HANDLER);

        SPELL_EDITOR_SCREEN_HANDLER = new MenuType<>((syncId, inventory) -> new SpellEditorScreenHandler(syncId, inventory), FeatureFlags.VANILLA_SET);
        Identifier editorId = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "spell_editor");
        Registry.register(BuiltInRegistries.MENU, editorId, SPELL_EDITOR_SCREEN_HANDLER);
    }

    public static void init() {}
}
