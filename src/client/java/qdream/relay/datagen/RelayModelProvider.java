package qdream.relay.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;

import qdream.relay.blocks.RelayBlocks;
import qdream.relay.items.RelayItems;

/**
 * Relay 模组模型生成器
 */
public class RelayModelProvider extends FabricModelProvider {
    public RelayModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators gen) {
        // 生成简单的立方体方块模型
        gen.createTrivialCube(RelayBlocks.SHELL_BLOCK);
        gen.createTrivialCube(RelayBlocks.SPELL_EDITOR_BLOCK);
    }

    @Override
    public void generateItemModels(ItemModelGenerators gen) {
        // 计算核心系列
        gen.generateFlatItem(RelayItems.COMPUTING_CORE_1, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.COMPUTING_CORE_2, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.COMPUTING_CORE_4, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.COMPUTING_CORE_8, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.COMPUTING_CORE_16, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.COMPUTING_CORE_32, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.COMPUTING_CORE_64, ModelTemplates.FLAT_ITEM);
        
        // 法术磁盘
        gen.generateFlatItem(RelayItems.SPELL_DISK, ModelTemplates.FLAT_ITEM);
        
        // 能量模块
        gen.generateFlatItem(RelayItems.ENERGY_MODULE, ModelTemplates.FLAT_ITEM);
        
        // 外壳物品
        gen.generateFlatItem(RelayItems.BLOCK_SHELL, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.ENTITY_SHELL, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.TOOL_SHELL, ModelTemplates.FLAT_ITEM);
    }
}
