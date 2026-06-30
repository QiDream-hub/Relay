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

                // ===== 计算核心系列 - 升级链 =====
                // 基础核心：3 铁锭 + 2 红石 + 1 绿宝石
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_1, 1)
                    .pattern("III")
                    .pattern("RAR")
                    .pattern("   ")
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .define('A', Items.AMETHYST_SHARD)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);

                // 升级配方：3 个低级核心 + 配料 = 1 个高级核心
                // CORE_1 (64tick) -> CORE_2 (32tick): 3 个 CORE_1 + 2 红石 + 1 青金石
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_2, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" L ")
                    .define('C', RelayItems.COMPUTING_CORE_1)
                    .define('R', Items.REDSTONE)
                    .define('L', Items.LAPIS_LAZULI)
                    .unlockedBy(getHasName(RelayItems.COMPUTING_CORE_1), has(RelayItems.COMPUTING_CORE_1))
                    .save(output);

                // CORE_2 (32tick) -> CORE_4 (16tick): 3 个 CORE_2 + 2 红石 + 1 黄金锭
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_4, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" G ")
                    .define('C', RelayItems.COMPUTING_CORE_2)
                    .define('R', Items.REDSTONE)
                    .define('G', Items.GOLD_INGOT)
                    .unlockedBy(getHasName(RelayItems.COMPUTING_CORE_2), has(RelayItems.COMPUTING_CORE_2))
                    .save(output);

                // CORE_4 (16tick) -> CORE_8 (8tick): 3 个 CORE_4 + 2 红石 + 1 下界石英
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_8, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" Q ")
                    .define('C', RelayItems.COMPUTING_CORE_4)
                    .define('R', Items.REDSTONE)
                    .define('Q', Items.QUARTZ)
                    .unlockedBy(getHasName(RelayItems.COMPUTING_CORE_4), has(RelayItems.COMPUTING_CORE_4))
                    .save(output);

                // CORE_8 (8tick) -> CORE_16 (4tick): 3 个 CORE_8 + 2 红石 + 1 末影珍珠
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_16, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" E ")
                    .define('C', RelayItems.COMPUTING_CORE_8)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.ENDER_PEARL)
                    .unlockedBy(getHasName(RelayItems.COMPUTING_CORE_8), has(RelayItems.COMPUTING_CORE_8))
                    .save(output);

                // CORE_16 (4tick) -> CORE_32 (2tick): 3 个 CORE_16 + 2 红石 + 1 烈焰棒
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_32, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" B ")
                    .define('C', RelayItems.COMPUTING_CORE_16)
                    .define('R', Items.REDSTONE)
                    .define('B', Items.BLAZE_ROD)
                    .unlockedBy(getHasName(RelayItems.COMPUTING_CORE_16), has(RelayItems.COMPUTING_CORE_16))
                    .save(output);

                // CORE_32 (2tick) -> CORE_64 (1tick): 3 个 CORE_32 + 2 红石 + 1 下界之星
                shapedRecipe(itemLookup, RelayItems.COMPUTING_CORE_64, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" S ")
                    .define('C', RelayItems.COMPUTING_CORE_32)
                    .define('R', Items.REDSTONE)
                    .define('S', Items.NETHER_STAR)
                    .unlockedBy(getHasName(RelayItems.COMPUTING_CORE_32), has(RelayItems.COMPUTING_CORE_32))
                    .save(output);
                
                // ===== 法术磁盘 =====
                shapedRecipe(itemLookup, RelayItems.SPELL_DISK, 4)
                    .pattern("IAI")
                    .pattern("IRI")
                    .pattern("   ")
                    .define('A', Items.AMETHYST_SHARD)
                    .define('I', Items.IRON_INGOT)
                    .define('R', Items.REDSTONE)
                    .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                    .save(output);

                // 磁盘清空配方：1 个磁盘合成 1 个空磁盘（清除 DataComponent）
                shapedRecipe(itemLookup, RelayItems.SPELL_DISK, 1)
                    .pattern("D ")
                    .define('D', RelayItems.SPELL_DISK)
                    .unlockedBy(getHasName(RelayItems.SPELL_DISK), has(RelayItems.SPELL_DISK))
                    .save(output, "spell_disk_clear");

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
                // 方块外壳：4 铁锭 + 4 紫水晶 + 2 红石 + 1 钻石
                shapedRecipe(itemLookup, RelayItems.BLOCK_SHELL, 1)
                    .pattern("ARA")
                    .pattern("RDR")
                    .pattern("AAA")
                    .define('A', Items.AMETHYST_SHARD)
                    .define('R', Items.REDSTONE)
                    .define('D', Items.DIAMOND)
                    .unlockedBy(getHasName(Items.AMETHYST_SHARD), has(Items.AMETHYST_SHARD))
                    .save(output);

                // 工具外壳
                shapedRecipe(itemLookup, RelayItems.TOOL_SHELL, 1)
                    .pattern("  A")
                    .pattern(" II")
                    .pattern("II ")
                    .define('A', Items.AMETHYST_SHARD)
                    .define('I', Items.IRON_INGOT)
                    .unlockedBy(getHasName(Items.AMETHYST_SHARD), has(Items.AMETHYST_SHARD))
                    .save(output);

                // ===== 世界交互器系列 =====
                // 基础配方：末影珍珠 + 运算核心 1 + 紫水晶
                shapedRecipe(itemLookup, RelayItems.WORLD_INTERACTOR_1, 1)
                    .pattern(" E ")
                    .pattern("PCP")
                    .pattern("   ")
                    .define('E', Items.ENDER_PEARL)
                    .define('P', Items.AMETHYST_SHARD)
                    .define('C', RelayItems.COMPUTING_CORE_1)
                    .unlockedBy(getHasName(Items.ENDER_PEARL), has(Items.ENDER_PEARL))
                    .save(output);

                // 升级配方：2 个低阶 + 配料 = 1 个高阶
                // INTERACTOR_1 -> INTERACTOR_2: 2 个 INTERACTOR_1 + 2 红石 + 1 青金石
                shapedRecipe(itemLookup, RelayItems.WORLD_INTERACTOR_2, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" L ")
                    .define('C', RelayItems.WORLD_INTERACTOR_1)
                    .define('R', Items.REDSTONE)
                    .define('L', Items.LAPIS_LAZULI)
                    .unlockedBy(getHasName(RelayItems.WORLD_INTERACTOR_1), has(RelayItems.WORLD_INTERACTOR_1))
                    .save(output);

                // INTERACTOR_2 -> INTERACTOR_4: 2 个 INTERACTOR_2 + 2 红石 + 1 黄金锭
                shapedRecipe(itemLookup, RelayItems.WORLD_INTERACTOR_4, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" G ")
                    .define('C', RelayItems.WORLD_INTERACTOR_2)
                    .define('R', Items.REDSTONE)
                    .define('G', Items.GOLD_INGOT)
                    .unlockedBy(getHasName(RelayItems.WORLD_INTERACTOR_2), has(RelayItems.WORLD_INTERACTOR_2))
                    .save(output);

                // INTERACTOR_4 -> INTERACTOR_8: 2 个 INTERACTOR_4 + 2 红石 + 1 下界石英
                shapedRecipe(itemLookup, RelayItems.WORLD_INTERACTOR_8, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" Q ")
                    .define('C', RelayItems.WORLD_INTERACTOR_4)
                    .define('R', Items.REDSTONE)
                    .define('Q', Items.QUARTZ)
                    .unlockedBy(getHasName(RelayItems.WORLD_INTERACTOR_4), has(RelayItems.WORLD_INTERACTOR_4))
                    .save(output);

                // INTERACTOR_8 -> INTERACTOR_16: 2 个 INTERACTOR_8 + 2 红石 + 1 末影珍珠
                shapedRecipe(itemLookup, RelayItems.WORLD_INTERACTOR_16, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" E ")
                    .define('C', RelayItems.WORLD_INTERACTOR_8)
                    .define('R', Items.REDSTONE)
                    .define('E', Items.ENDER_PEARL)
                    .unlockedBy(getHasName(RelayItems.WORLD_INTERACTOR_8), has(RelayItems.WORLD_INTERACTOR_8))
                    .save(output);

                // INTERACTOR_16 -> INTERACTOR_32: 2 个 INTERACTOR_16 + 2 红石 + 1 烈焰棒
                shapedRecipe(itemLookup, RelayItems.WORLD_INTERACTOR_32, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" B ")
                    .define('C', RelayItems.WORLD_INTERACTOR_16)
                    .define('R', Items.REDSTONE)
                    .define('B', Items.BLAZE_ROD)
                    .unlockedBy(getHasName(RelayItems.WORLD_INTERACTOR_16), has(RelayItems.WORLD_INTERACTOR_16))
                    .save(output);

                // INTERACTOR_32 -> INTERACTOR_64: 2 个 INTERACTOR_32 + 2 红石 + 1 下界之星
                shapedRecipe(itemLookup, RelayItems.WORLD_INTERACTOR_64, 1)
                    .pattern(" C ")
                    .pattern("RCR")
                    .pattern(" S ")
                    .define('C', RelayItems.WORLD_INTERACTOR_32)
                    .define('R', Items.REDSTONE)
                    .define('S', Items.NETHER_STAR)
                    .unlockedBy(getHasName(RelayItems.WORLD_INTERACTOR_32), has(RelayItems.WORLD_INTERACTOR_32))
                    .save(output);

                // ===== 法术编辑器 =====
                // 法术编辑器方块：4 金锭 + 2 红石 + 2 紫水晶 + 1 运算核心 1
                shapedRecipe(itemLookup, RelayBlocks.SPELL_EDITOR_BLOCK.asItem(), 1)
                    .pattern(" G ")
                    .pattern("GCG")
                    .pattern(" R ")
                    .define('G', Items.GOLD_INGOT)
                    .define('C', RelayItems.COMPUTING_CORE_1)
                    .define('R', Items.REDSTONE)
                    .unlockedBy(getHasName(RelayItems.SPELL_DISK), has(RelayItems.SPELL_DISK))
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
