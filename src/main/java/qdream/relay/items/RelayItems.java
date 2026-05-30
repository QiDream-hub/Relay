package qdream.relay.items;

import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 物品注册表
 */
public class RelayItems {

    public static final Item COMPUTING_CORE = new ComputingCoreItem();
    public static final Item SPELL_DISK = new SpellDiskItem();
    public static final Item ENERGY_MODULE = new EnergyModuleItem();

    public static void init() {
        register("computing_core", COMPUTING_CORE);
        register("spell_disk", SPELL_DISK);
        register("energy_module", ENERGY_MODULE);
    }

    private static Item register(String path, Item item) {
        Identifier id = Identifier.fromNamespaceAndPath(Relay.MOD_ID, path);
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }
}
