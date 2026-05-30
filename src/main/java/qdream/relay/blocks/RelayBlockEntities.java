package qdream.relay.blocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;
import qdream.relay.blocks.entity.ShellBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

/**
 * 方块实体注册表
 */
public class RelayBlockEntities {

    public static final BlockEntityType<ShellBlockEntity> SHELL_BLOCK_ENTITY;

    static {
        SHELL_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(ShellBlockEntity::new, RelayBlocks.SHELL_BLOCK).build(null);
        Identifier id = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "shell");
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, SHELL_BLOCK_ENTITY);
    }

    public static void init() {}
}
