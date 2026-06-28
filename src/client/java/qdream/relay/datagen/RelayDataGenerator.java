package qdream.relay.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Relay 模组 Data Generator 入口点
 */
public class RelayDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        
        // 注册配方生成器
        pack.addProvider(RelayRecipeProvider::new);
        
        // 注册模型生成器
        pack.addProvider(RelayModelProvider::new);
    }
}
