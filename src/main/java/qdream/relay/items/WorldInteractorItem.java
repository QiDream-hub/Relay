package qdream.relay.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import qdream.relay.Component.RelayDataComponents;
import qdream.relay.mc.component.WorldInteractorComponent;

/**
 * 世界交互器物品
 * 提供世界交互能力（方块放置/破坏、实体交互等）
 *
 * 特性：
 * - 64 个品阶（1-64），品阶数字越大，交互距离越远，能量消耗越高
 * - 需要持续消耗能量才能工作
 * - 与运算核心共享能量模块
 */
public class WorldInteractorItem extends Item implements WorldInteractorComponent {

    /**
     * 基础交互距离（品阶 1）
     */
    private static final double BASE_RANGE = 4.0;

    /**
     * 基础能量消耗系数
     */
    private static final double ENERGY_COST_FACTOR = 0.5;

    /**
     * 固定品阶值
     */
    private final int fixedTier;

    /**
     * 固定能量消耗
     */
    private final double fixedEnergyCost;

    public WorldInteractorItem(Properties properties, int tier) {
        super(properties);
        this.fixedTier = tier;
        // 能量消耗与品阶的平方根成正比：sqrt(tier) * 0.5
        // 品阶 1: 0.5 能量
        // 品阶 64: 4.0 能量
        this.fixedEnergyCost = Math.sqrt(tier) * ENERGY_COST_FACTOR;
    }

    /**
     * 获取世界交互器的品阶
     *
     * @param stack 物品堆
     * @return 品阶值（1-64）
     */
    public int getTier(ItemStack stack) {
        Integer tier = stack.get(RelayDataComponents.WORLD_INTERACTOR_TIER);
        return tier != null ? tier : fixedTier;
    }

    /**
     * 设置世界交互器的品阶
     *
     * @param stack 物品堆
     * @param tier  品阶值（1-64）
     * @return 是否设置成功（固定品阶返回 false）
     */
    public boolean setTier(ItemStack stack, int tier) {
        return false;
    }

    /**
     * 获取世界交互器的交互距离
     *
     * @param stack 物品堆
     * @return 交互距离（方块单位）
     */
    @Override
    public double getRange(ItemStack stack) {
        Double range = stack.get(RelayDataComponents.WORLD_INTERACTION_RANGE);
        return range != null ? range : (BASE_RANGE + fixedTier * 2);
    }

    /**
     * 设置世界交互器的交互距离
     *
     * @param stack 物品堆
     * @param range 交互距离
     * @return 是否设置成功（固定距离返回 false）
     */
    @Override
    public boolean setRange(ItemStack stack, double range) {
        return false;
    }

    /**
     * 获取世界交互器的能量消耗
     *
     * @param stack 物品堆
     * @return 每次交互消耗的能量
     */
    @Override
    public double getEnergyCost(ItemStack stack) {
        Double energyCost = stack.get(RelayDataComponents.ENERGY_COST);
        return energyCost != null ? energyCost : fixedEnergyCost;
    }

    /**
     * 设置世界交互器的能量消耗
     *
     * @param stack      物品堆
     * @param energyCost 能量消耗值
     * @return 是否设置成功（固定能量消耗返回 false）
     */
    @Override
    public boolean setEnergyCost(ItemStack stack, double energyCost) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
            Consumer<Component> textConsumer, TooltipFlag flag) {
        int tier = getTier(stack);
        double range = getRange(stack);
        double energyCost = getEnergyCost(stack);

        textConsumer.accept(
                Component.translatable("item.relay.world_interactor.tier", tier)
                        .withStyle(ChatFormatting.GOLD));
        textConsumer.accept(
                Component.translatable("item.relay.world_interactor.range", String.format("%.1f", range))
                        .withStyle(ChatFormatting.AQUA));
        textConsumer.accept(
                Component.translatable("item.relay.world_interactor.energy_cost", String.format("%.1f", energyCost))
                        .withStyle(ChatFormatting.RED));
        textConsumer.accept(
                Component.translatable("item.relay.world_interactor.desc")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }
}
