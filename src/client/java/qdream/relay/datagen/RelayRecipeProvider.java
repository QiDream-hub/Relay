package qdream.relay.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;

import qdream.relay.Relay;
import qdream.relay.blocks.RelayBlocks;
import qdream.relay.items.RelayItems;

/**
 * Relay 模组配方生成器
 */
public class RelayRecipeProvider extends FabricRecipeProvider {
    public RelayRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registryLookup, RecipeOutput exporter) {
        return new RecipeProvider(registryLookup, exporter) {
            @Override
            public void buildRecipes() {
                HolderLookup.RegistryLookup<Item> itemLookup = registries.lookupOrThrow(Registries.ITEM);
                
                // ===== 计算核心系列 =====
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_1, 1)
                    .pattern("III")
                    .pattern("RER")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.EMERALD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_2, 1)
                    .pattern("III")
                    .pattern("RER")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.EMERALD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_4, 1)
                    .pattern("III")
                    .pattern("RER")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.EMERALD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_8, 1)
                    .pattern("III")
                    .pattern("RER")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.EMERALD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_16, 1)
                    .pattern("III")
                    .pattern("RER")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.EMERALD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_32, 1)
                    .pattern("III")
                    .pattern("RER")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.EMERALD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_64, 1)
                    .pattern("III")
                    .pattern("RER")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.EMERALD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                // ===== 法术磁盘 =====
                shapedRecipe(itemLookup, RelayItems.SPELL_DISK, 4)
                    .pattern("III")
                    .pattern("RER")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.EMERALD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                // ===== 能量模块 =====
                shapedRecipe(itemLookup, RelayItems.ENERGY_MODULE, 1)
                    .pattern(" G ")
                    .pattern("GRG")
                    .pattern(" G ")
                    .define('G', Items.GOLD_INGOT)
                    .define('R', Items.REDSTONE)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                // ===== 外壳物品 =====
                // 方块外壳
                shapedRecipe(itemLookup, RelayItems.BLOCK_SHELL, 1)
                    .pattern("III")
                    .pattern("I I")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                    .save(output);
                
                // 实体外壳
                shapedRecipe(itemLookup, RelayItems.ENTITY_SHELL, 1)
                    .pattern("III")
                    .pattern("RER")
                    .pattern("III")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.AMETHYST_SHARD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);
                
                // 工具外壳
                shapedRecipe(itemLookup, RelayItems.TOOL_SHELL, 1)
                    .pattern("  G")
                    .pattern(" II")
                    .pattern("II ")
                    .define('G', Items.GOLD_INGOT)
                    .define('I', Items.IRON_INGOT)
                    .unlockedBy(getHasName(Items.GOLD_INGOT), has(Items.GOLD_INGOT))
                    .save(output);
            }
            
            private ShapedRecipeBuilder shapedRecipe(HolderLookup.RegistryLookup<Item> itemLookup, Item result, int count) {
                return ShapedRecipeBuilder.shaped(itemLookup, RecipeCategory.MISC, result, count);
            }
        };
    }

    @Override
    public String getName() {
        return "RelayRecipeProvider";
    }
}
