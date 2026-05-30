package qdream.relay.items;

import net.fabricmc.api.ModInitializer;

/**
 * 运算核心物品
 * 提供操作数预算，可设置 interval（1-100）
 */
public class ComputingCoreItem implements ModInitializer{

    @Override
    public void onInitialize() {
        RelayItems.register();
    }
}
