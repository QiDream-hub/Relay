package qdream.relay.items;

import net.minecraft.world.item.Item;

/**
 * 运算核心物品
 * 提供操作数预算，可设置 interval（1-100）
 */
public class ComputingCoreItem extends Item {
    public ComputingCoreItem() {
        super(new Properties().stacksTo(1));
    }
}
