package qdream.relay.items;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import qdream.relay.Relay;
import qdream.relay.blocks.RelayBlocks;

/**
 * 物品注册表
 */
public class RelayItems {
    public static final Item COMPUTING_CORE = register("computing_core", Item::new, new Item.Properties());
    public static final Item SPELL_DISK = register("spell_disk", SpellDiskItem::new, new Item.Properties());
    public static final Item ENERGY_MODULE = register("energy_module", Item::new, new Item.Properties());

    // 三种外壳物品
    public static final Item BLOCK_SHELL = register("block_shell", Item::new, new Item.Properties());
    public static final Item ENTITY_SHELL = register("entity_shell", Item::new, new Item.Properties());
    public static final Item TOOL_SHELL = register("tool_shell", ToolShellItem::new, new Item.Properties());

    public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory,
            Item.Properties settings) {
        // Create the item key.
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Relay.MOD_ID, name));

        // Create the item instance.
        T item = itemFactory.apply(settings.setId(itemKey));

        // Register the item.
        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static final ResourceKey<CreativeModeTab> CUSTOM_CREATIVE_TAB_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(), Identifier.fromNamespaceAndPath(Relay.MOD_ID, "creative_tab"));
    public static final CreativeModeTab CUSTOM_CREATIVE_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(RelayItems.COMPUTING_CORE))
            .title(Component.translatable("Relay"))
            .displayItems((params, output) -> {
                // 核心组件
                output.accept(RelayItems.COMPUTING_CORE);
                output.accept(RelayItems.ENERGY_MODULE);
                output.accept(RelayItems.SPELL_DISK);

                // 方块
                output.accept(RelayBlocks.SPELL_EDITOR_BLOCK);
                output.accept(RelayBlocks.SHELL_BLOCK);
                output.accept(RelayItems.ENTITY_SHELL);
                output.accept(RelayItems.TOOL_SHELL);
            })
            .build();

    public static void register() {
        // Register the group.
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CUSTOM_CREATIVE_TAB_KEY, CUSTOM_CREATIVE_TAB);
    }

}