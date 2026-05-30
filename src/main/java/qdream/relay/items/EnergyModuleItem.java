package qdream.relay.items;

import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.Item;

/**
 * 能量模块物品
 * 存储紫水晶能量
 */
public class EnergyModuleItem implements ModInitializer{

    @Override
    public void onInitialize() {
        RelayItems.register();
    }
    
}
