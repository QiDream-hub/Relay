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
    public static final int AMETHYST_SHARD_ENERGY = 25;
    
    /**
     * 紫水晶块提供的能量
     */
    public static final int AMETHYST_BLOCK_ENERGY = 250;
    
    /**
     * 默认最大能量存储
     */
    public static final int DEFAULT_MAX_ENERGY = 10000;

    private EnergySystem() {}

    /**
     * 从物品堆中提取能量
     * @param stack 物品堆
     * @return 提取的能量值
     */
    public static int extractEnergy(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        
        // 紫水晶碎片
        if (stack.is(Items.AMETHYST_SHARD)) {
            int amount = Math.min(stack.getCount(), 64);
            stack.shrink(amount);
            return amount * AMETHYST_SHARD_ENERGY;
        }
        
        // 紫水晶块
        if (stack.is(Items.AMETHYST_BLOCK)) {
            int amount = Math.min(stack.getCount(), 64);
            stack.shrink(amount);
            return amount * AMETHYST_BLOCK_ENERGY;
        }
        
        return 0;
    }

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
    public static int getEnergyValue(ItemStack stack) {
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
