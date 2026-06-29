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
        gen.createTrivialCube(RelayBlocks.BLOCK_SHELL_BLOCK);
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

        // 世界交互器系列
        gen.generateFlatItem(RelayItems.WORLD_INTERACTOR_1, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.WORLD_INTERACTOR_2, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.WORLD_INTERACTOR_4, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.WORLD_INTERACTOR_8, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.WORLD_INTERACTOR_16, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.WORLD_INTERACTOR_32, ModelTemplates.FLAT_ITEM);
        gen.generateFlatItem(RelayItems.WORLD_INTERACTOR_64, ModelTemplates.FLAT_ITEM);

        // 方块外壳 - 物品模型手动创建在 resources/assets/relay/items/ 目录
        // 因为需要引用方块模型而非物品纹理

        // 工具外壳使用物品纹理
        gen.generateFlatItem(RelayItems.TOOL_SHELL, ModelTemplates.FLAT_ITEM);
    }
}
