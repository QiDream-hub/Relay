package qdream.relay.items;

import net.minecraft.world.item.ItemStack;
import qdream.relay.Component.RelayDataComponents;

public class DynamicIntervalCoreItem extends ComputingCoreItem {

    public DynamicIntervalCoreItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getInterval(ItemStack stack) {
        Integer integer = stack.get(RelayDataComponents.INTERVAL);
        return integer == null ? 1 : integer;
    }

    @Override
    public boolean setInterval(ItemStack stack, int interval) {
        stack.set(RelayDataComponents.INTERVAL, Math.max(interval, 1));
        return true;
    }

    @Override
    public int getCost(ItemStack stack) {
        Integer integer = stack.get(RelayDataComponents.COST);
        return integer == null ? 1 : integer;
    }

    @Override
    public boolean setCost(ItemStack stack, int cost) {
        stack.set(RelayDataComponents.COST, Math.max(cost, 1));
        return true;
    }

    @Override
    public double getEnergyCost(ItemStack stack) {
        double interval = 20 / getInterval(stack);
        double cost = getCost(stack) * 1.5;
        return interval + cost;
    }

    @Override
    public boolean setEnergyCost(ItemStack stack, double energyCost) {
        return false;
    }

}
