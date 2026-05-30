package qdream.relay.client;

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

    static {
        SHELL_SCREEN_HANDLER = new MenuType<>((syncId, inventory) -> new ShellScreenHandler(syncId, inventory), FeatureFlags.VANILLA_SET);
        Identifier id = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "shell");
        Registry.register(BuiltInRegistries.MENU, id, SHELL_SCREEN_HANDLER);
    }

    public static void init() {}
}
