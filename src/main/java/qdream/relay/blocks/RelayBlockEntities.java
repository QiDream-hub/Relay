package qdream.relay.blocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;
import qdream.relay.blocks.entity.ShellBlockEntity;
import qdream.relay.blocks.entity.SpellEditorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

/**
 * 方块实体注册表
 */
public class RelayBlockEntities {

    public static final BlockEntityType<ShellBlockEntity> SHELL_BLOCK_ENTITY;
    public static final BlockEntityType<SpellEditorBlockEntity> SPELL_EDITOR_BLOCK_ENTITY;

    static {
        SHELL_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(ShellBlockEntity::new, RelayBlocks.SHELL_BLOCK).build(null);
        Identifier shellId = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "shell");
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, shellId, SHELL_BLOCK_ENTITY);

        SPELL_EDITOR_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(SpellEditorBlockEntity::new, RelayBlocks.SPELL_EDITOR_BLOCK).build(null);
        Identifier spellEditorId = Identifier.fromNamespaceAndPath(Relay.MOD_ID, "spell_editor");
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, spellEditorId, SPELL_EDITOR_BLOCK_ENTITY);
    }

    public static void register() {}
}
