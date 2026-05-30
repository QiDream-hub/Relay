package qdream.relay.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 方块注册表
 */
public class RelayBlocks {

    public static final Block SHELL_BLOCK = new ShellBlock(Block.Properties.of());

    public static void init() {
        register("shell", SHELL_BLOCK);
    }

    private static void register(String path, Block block) {
        Identifier id = Identifier.fromNamespaceAndPath(Relay.MOD_ID, path);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
    }
}
