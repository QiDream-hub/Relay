package qdream.relay.mc.component;

import net.minecraft.world.item.ItemStack;

/**
 * 能量模块组件接口
 * 提供能量存储和充能方法
 */
public interface EnergyModuleComponent {

    /**
     * 获取当前存储的能量
     *
     * @param stack 物品堆
     * @return 能量值
     */
    double getStoredEnergy(ItemStack stack);

    /**
     * 设置能量值
     *
     * @param stack 物品堆
     * @param energy 能量值
     */
    void setStoredEnergy(ItemStack stack, double energy);

    /**
     * 添加能量
     *
     * @param stack  物品堆
     * @param amount 添加量
     * @return 实际添加的能量值
     */
    double addEnergy(ItemStack stack, double amount);

    /**
     * 消耗能量
     *
     * @param stack  物品堆
     * @param amount 消耗量
     * @return 实际消耗的能量值
     */
    double consumeEnergy(ItemStack stack, double amount);

    /**
     * 检查是否有足够能量
     *
     * @param stack  物品堆
     * @param amount 需要的能量值
     * @return 如果有足够能量返回 true
     */
    boolean hasEnergy(ItemStack stack, double amount);
}
