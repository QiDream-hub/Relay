package qdream.relay.items;

import net.minecraft.world.item.ItemStack;
import qdream.relay.Component.RelayDataComponents;

/**
 * 固定间隔的计算核心物品
 * 当前 7 种核心使用此实现（64/32/16/8/4/2/1）
 * energyCost 与 interval 倒序对应（interval 64→energyCost 0.5, interval 1→energyCost 32）
 */
public class FixedIntervalCoreItem extends ComputingCoreItem {

    private final int fixedInterval;
    private final double fixedEnergyCost;

    public FixedIntervalCoreItem(Properties properties, int interval) {
        super(properties);
        this.fixedInterval = interval;
        // energyCost 与 interval 倒序：32.0/interval
        // 核心_1 (interval=64): 0.5 能量
        // 核心_64 (interval=1): 32 能量
        this.fixedEnergyCost = 32.0 / interval;
    }

    @Override
    public int getInterval(ItemStack stack) {
        // 优先读取 DataComponent（允许动态修改），否则返回固定值
        Integer interval = stack.get(RelayDataComponents.INTERVAL);
        return interval != null ? interval : fixedInterval;
    }

    @Override
    public boolean setInterval(ItemStack stack, int interval) {
        // 固定间隔核心不允许修改，返回 false
        return false;
    }

    @Override
    public double getEnergyCost(ItemStack stack) {
        // 优先读取 DataComponent（允许动态修改），否则返回固定值
        Double energyCost = stack.get(RelayDataComponents.ENERGY_COST);
        return energyCost != null ? energyCost : fixedEnergyCost;
    }

    @Override
    public boolean setEnergyCost(ItemStack stack, double energyCost) {
        // 固定能量消耗核心不允许修改，返回 false
        return false;
    }
}
