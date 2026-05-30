package qdream.relay.items;

import net.minecraft.world.item.Item;

/**
 * 法术磁盘物品
 * 存储栈图程序（Iota 列表）
 */
public class SpellDiskItem extends Item {
    public SpellDiskItem() {
        super(new Properties().stacksTo(1));
    }
}
