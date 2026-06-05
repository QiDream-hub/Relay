package qdream.relay.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

import qdream.relay.Relay;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;

/**
 * 实体注册表
 */
public class RelayEntityTypes {

    public static final EntityType<SimpleEntityShell> SIMPLE_ENTITY_SHELL;

    static {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Relay.MOD_ID, "entity_shell"));
        SIMPLE_ENTITY_SHELL = FabricEntityTypeBuilder.<SimpleEntityShell>create(MobCategory.MISC, SimpleEntityShell::new)
                .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
                .trackRangeBlocks(64)
                .trackedUpdateRate(1)
                .build(key);
        
        Registry.register(BuiltInRegistries.ENTITY_TYPE, key, SIMPLE_ENTITY_SHELL);
    }

    public static void register() {}
}
