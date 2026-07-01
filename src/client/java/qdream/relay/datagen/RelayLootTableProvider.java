package qdream.relay.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootTable;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;

import qdream.relay.blocks.RelayBlocks;

/**
 * Relay 模组战利品表生成器
 * 为所有方块生成掉落物表
 */
public class RelayLootTableProvider extends FabricBlockLootSubProvider {

    public RelayLootTableProvider(FabricPackOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        // 方块外壳 - 掉落自身
        this.dropSelf(RelayBlocks.BLOCK_SHELL_BLOCK);

        // 法术编辑器方块 - 掉落自身
        this.dropSelf(RelayBlocks.SPELL_EDITOR_BLOCK);
    }
}
