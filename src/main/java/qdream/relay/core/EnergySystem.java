package qdream.relay.core;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 能量系统
 * 管理紫水晶能量的存储和消耗
 */
public class EnergySystem {

    /**
     * 紫水晶碎片提供的能量
     */
    public static final double AMETHYST_SHARD_ENERGY = 25.0;

    /**
     * 紫水晶块提供的能量
     */
    public static final double AMETHYST_BLOCK_ENERGY = 250.0;

    private EnergySystem() {}

    /**
     * 检查物品堆是否含有能量
     * @param stack 物品堆
     * @return 是否含有能量
     */
    public static boolean hasEnergy(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.is(Items.AMETHYST_SHARD) || stack.is(Items.AMETHYST_BLOCK);
    }

    /**
     * 获取物品堆的能量值
     * @param stack 物品堆
     * @return 能量值
     */
    public static double getEnergyValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        if (stack.is(Items.AMETHYST_SHARD)) {
            return stack.getCount() * AMETHYST_SHARD_ENERGY;
        }

        if (stack.is(Items.AMETHYST_BLOCK)) {
            return stack.getCount() * AMETHYST_BLOCK_ENERGY;
        }

        return 0;
    }
}
