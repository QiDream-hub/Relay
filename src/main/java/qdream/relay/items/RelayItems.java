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
    // 计算核心系列 - 名称数字与 interval 成反比（名称 1→64，名称 64→1）
    // 名称数字越大，性能越强（interval 越小，energyCost 越高）
    public static final Item COMPUTING_CORE_1 = register("computing_core_1",
            props -> new FixedIntervalCoreItem(props, 64), new Item.Properties());
    public static final Item COMPUTING_CORE_2 = register("computing_core_2",
            props -> new FixedIntervalCoreItem(props, 32), new Item.Properties());
    public static final Item COMPUTING_CORE_4 = register("computing_core_4",
            props -> new FixedIntervalCoreItem(props, 16), new Item.Properties());
    public static final Item COMPUTING_CORE_8 = register("computing_core_8",
            props -> new FixedIntervalCoreItem(props, 8), new Item.Properties());
    public static final Item COMPUTING_CORE_16 = register("computing_core_16",
            props -> new FixedIntervalCoreItem(props, 4), new Item.Properties());
    public static final Item COMPUTING_CORE_32 = register("computing_core_32",
            props -> new FixedIntervalCoreItem(props, 2), new Item.Properties());
    public static final Item COMPUTING_CORE_64 = register("computing_core_64",
            props -> new FixedIntervalCoreItem(props, 1), new Item.Properties());

    public static final Item SPELL_DISK = register("spell_disk", SpellDiskItem::new, new Item.Properties());
    public static final Item ENERGY_MODULE = register("energy_module", EnergyModuleItem::new, new Item.Properties());

    // 世界交互器系列 - 品阶 1-64（品阶数字越大，交互距离越远，能量消耗越高）
    public static final Item WORLD_INTERACTOR_1 = register("world_interactor_1",
            props -> new WorldInteractorItem(props, 1), new Item.Properties());
    public static final Item WORLD_INTERACTOR_2 = register("world_interactor_2",
            props -> new WorldInteractorItem(props, 2), new Item.Properties());
    public static final Item WORLD_INTERACTOR_4 = register("world_interactor_4",
            props -> new WorldInteractorItem(props, 4), new Item.Properties());
    public static final Item WORLD_INTERACTOR_8 = register("world_interactor_8",
            props -> new WorldInteractorItem(props, 8), new Item.Properties());
    public static final Item WORLD_INTERACTOR_16 = register("world_interactor_16",
            props -> new WorldInteractorItem(props, 16), new Item.Properties());
    public static final Item WORLD_INTERACTOR_32 = register("world_interactor_32",
            props -> new WorldInteractorItem(props, 32), new Item.Properties());
    public static final Item WORLD_INTERACTOR_64 = register("world_interactor_64",
            props -> new WorldInteractorItem(props, 64), new Item.Properties());

    // 三种外壳物品
    // block_shell = 方块外壳，可以放置成方块（使用 BlockItem）
    // entity_shell = 实体外壳，使用后生成实体
    // tool_shell = 工具外壳，手持使用的工具
    public static final Item BLOCK_SHELL = RelayBlocks.BLOCK_SHELL_BLOCK.asItem();
    public static final Item TOOL_SHELL = register("tool_shell", ToolShellItem::new, new Item.Properties());

    public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory,
            Item.Properties settings) {
        return register(name, itemFactory, settings, 0);
    }

    public static <T extends Item> T register(String name, Function<Item.Properties, T> itemFactory,
            Item.Properties settings, int interval) {
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
            .icon(() -> new ItemStack(RelayItems.COMPUTING_CORE_64))
            .title(Component.translatable("Relay"))
            .displayItems((params, output) -> {
                // 核心组件 - 计算核心系列（从低级到高级）
                output.accept(RelayItems.COMPUTING_CORE_1);
                output.accept(RelayItems.COMPUTING_CORE_2);
                output.accept(RelayItems.COMPUTING_CORE_4);
                output.accept(RelayItems.COMPUTING_CORE_8);
                output.accept(RelayItems.COMPUTING_CORE_16);
                output.accept(RelayItems.COMPUTING_CORE_32);
                output.accept(RelayItems.COMPUTING_CORE_64);
                output.accept(RelayItems.ENERGY_MODULE);
                output.accept(RelayItems.SPELL_DISK);

                // 世界交互器系列（从低级到高级）
                output.accept(RelayItems.WORLD_INTERACTOR_1);
                output.accept(RelayItems.WORLD_INTERACTOR_2);
                output.accept(RelayItems.WORLD_INTERACTOR_4);
                output.accept(RelayItems.WORLD_INTERACTOR_8);
                output.accept(RelayItems.WORLD_INTERACTOR_16);
                output.accept(RelayItems.WORLD_INTERACTOR_32);
                output.accept(RelayItems.WORLD_INTERACTOR_64);

                // 方块和外壳
                output.accept(RelayBlocks.BLOCK_SHELL_BLOCK);  // 方块外壳
                output.accept(RelayItems.TOOL_SHELL);          // 工具外壳
                output.accept(RelayBlocks.SPELL_EDITOR_BLOCK);
            })
            .build();

    public static void register() {
        // Register the group.
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CUSTOM_CREATIVE_TAB_KEY, CUSTOM_CREATIVE_TAB);
    }

}