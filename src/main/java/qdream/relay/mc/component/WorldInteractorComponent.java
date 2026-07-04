package qdream.relay.mc.component;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 世界交互器组件接口
 * 提供品阶、交互距离和范围检查方法
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

    /**
     * 检查目标坐标是否在交互范围内
     * 使用欧几里得距离判断
     *
     * @param stack     世界交互器物品堆
     * @param sourcePos 源坐标（交互器位置）
     * @param targetPos 目标坐标（操作位置）
     * @return 如果目标坐标在范围内返回 true，否则返回 false
     */
    default boolean isInRange(ItemStack stack, Vec3 sourcePos, Vec3 targetPos) {
        double range = getRange(stack);
        double distance = sourcePos.distanceTo(targetPos);
        return distance <= range;
    }
}
