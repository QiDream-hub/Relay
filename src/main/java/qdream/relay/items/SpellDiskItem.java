package qdream.relay.items;

import net.fabricmc.api.ModInitializer;

/**
 * 法术磁盘物品
 * 存储栈图程序（Iota 列表）
 */
public class SpellDiskItem implements ModInitializer{

	@Override
	public void onInitialize() {
		RelayItems.register();
	}
}