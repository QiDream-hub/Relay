package qdream.relay.items;

import net.minecraft.world.item.ItemStack;
import qdream.relay.Component.RelayDataComponents;

/**
 * 固定间隔的计算核心物品
 * 当前 7 种核心使用此实现（64/32/16/8/4/2/1）
 * 
 * <p>能量消耗使用 1.3 次方曲线：前期适中，后期升高</p>
 * <p>公式：{@code energyCost = baseEnergyCost * count^1.3}</p>
 * <p>其中 {@code baseEnergyCost = 1.0 / interval}</p>
 */
public class FixedIntervalCoreItem extends ComputingCoreItem {

    private final int fixedInterval;
    private final double baseEnergyCost;

    public FixedIntervalCoreItem(Properties properties, int interval) {
        super(properties);
        this.fixedInterval = interval;
        this.baseEnergyCost = 1.0 / interval;
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
        int count = stack.getCount();
        // 1.3 次方曲线：前期适中，后期升高
        return baseEnergyCost * Math.pow(count, 1.3);
    }

    @Override
    public boolean setEnergyCost(ItemStack stack, double energyCost) {
        // 固定能量消耗核心不允许修改，返回 false
        return false;
    }

    @Override
    public boolean setCost(ItemStack stack) {
        // 固定核心不允许修改，返回 false
        return false;
    }
}
