package qdream.relay.mc.component;

import net.minecraft.world.item.ItemStack;

/**
 * 运算核心组件接口
 * 提供执行间隔、成本和能量消耗方法
 */
public interface ComputingCoreComponent {

    /**
     * 获取执行间隔（tick 间隔）
     *
     * @param stack 物品堆
     * @return interval 值
     */
    int getInterval(ItemStack stack);

    /**
     * 设置执行间隔
     *
     * @param stack    物品堆
     * @param interval 间隔值
     * @return 是否设置成功
     */
    boolean setInterval(ItemStack stack, int interval);

    /**
     * 获取执行成本
     * 返回 ItemStack 的 count 数值
     *
     * @param stack 物品堆
     * @return cost 值
     */
    int getCost(ItemStack stack);

    /**
     * 获取能量消耗
     *
     * @param stack 物品堆
     * @return 能量消耗值
     */
    double getEnergyCost(ItemStack stack);

    /**
     * 设置能量消耗
     *
     * @param stack      物品堆
     * @param energyCost 能量消耗值
     * @return 是否设置成功
     */
    boolean setEnergyCost(ItemStack stack, double energyCost);
}
