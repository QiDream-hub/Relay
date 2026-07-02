package qdream.relay.mc.component;

import net.minecraft.world.item.ItemStack;

/**
 * 世界交互器组件接口
 * 提供品阶和交互距离方法
 */
public interface WorldInteractorComponent {

    /**
     * 获取世界交互器的交互距离
     *
     * @param stack 物品堆
     * @return 交互距离（方块单位）
     */
    double getRange(ItemStack stack);

    /**
     * 设置世界交互器的交互距离
     *
     * @param stack 物品堆
     * @param range 交互距离
     * @return 是否设置成功
     */
    boolean setRange(ItemStack stack, double range);

    /**
     * 获取世界交互器的能量消耗
     *
     * @param stack 物品堆
     * @return 每次交互消耗的能量
     */
    double getEnergyCost(ItemStack stack);

    /**
     * 设置世界交互器的能量消耗
     *
     * @param stack      物品堆
     * @param energyCost 能量消耗值
     * @return 是否设置成功
     */
    boolean setEnergyCost(ItemStack stack, double energyCost);
}
