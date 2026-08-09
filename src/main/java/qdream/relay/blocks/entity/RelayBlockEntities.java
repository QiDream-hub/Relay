package qdream.relay.blocks.entity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import qdream.relay.Relay;
import qdream.relay.blocks.RelayBlocks;
import qdream.relay.blocks.entity.custom.BlockShellEntity;
import qdream.relay.blocks.entity.custom.EditorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

/**
 * 方块实体注册表
 */
public class RelayBlockEntities {

    public static final BlockEntityType<BlockShellEntity> SHELL_BLOCK_ENTITY =
            register("shell", BlockShellEntity::new, RelayBlocks.BLOCK_SHELL_BLOCK);
    public static final BlockEntityType<EditorBlockEntity> SPELL_EDITOR_BLOCK_ENTITY =
            register("spell_editor", EditorBlockEntity::new, RelayBlocks.SPELL_EDITOR_BLOCK);

    /**
     * 泛型 helper 方法，用于注册方块实体类型
     */
    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> entityFactory,
            Block... blocks
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(Relay.MOD_ID, name);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
            FabricBlockEntityTypeBuilder.<T>create(entityFactory, blocks).build(null));
    }

    public static void register() {}
}
