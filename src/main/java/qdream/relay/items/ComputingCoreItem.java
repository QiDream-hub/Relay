package qdream.relay.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import qdream.relay.Component.RelayDataComponents;

/**
 * 运算核心物品基类
 * 提供操作数预算，interval 属性决定执行间隔（tick 间隔）
 * interval 值越小，执行频率越高
 * 
 * @see FixedIntervalCoreItem 固定 interval 子类（当前 7 种核心）
 * @see DynamicIntervalCoreItem 动态 interval 子类（未来特殊核心）
 */
public abstract class ComputingCoreItem extends Item {

    public ComputingCoreItem(Properties properties) {
        // 不设置 stacksTo，使用默认 64 堆叠
        super(properties);
    }

    /**
     * 获取计算核心的执行间隔
     * 
     * @param stack 物品堆
     * @return interval 值（tick 间隔）
     */
    public abstract int getInterval(ItemStack stack);

    /**
     * 设置计算核心的执行间隔
     *
     * @param stack    物品堆
     * @param interval 间隔值（tick 间隔）
     * @return 是否设置成功（固定 interval 返回 false）
     */
    public abstract boolean setInterval(ItemStack stack, int interval);

    /**
     * 获取计算核心的能量消耗
     *
     * @param stack 物品堆
     * @return energyCost 值（每次执行消耗的能量）
     */
    public abstract double getEnergyCost(ItemStack stack);

    /**
     * 设置计算核心的能量消耗
     *
     * @param stack      物品堆
     * @param energyCost 能量消耗值
     * @return 是否设置成功（固定 energyCost 返回 false）
     */
    public abstract boolean setEnergyCost(ItemStack stack, double energyCost);

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
            Consumer<Component> textConsumer, TooltipFlag type) {
        int interval = getInterval(stack);
        double tps = 20.0 / interval; // 每秒执行次数
        double energyCost = getEnergyCost(stack);

        textConsumer.accept(
                Component.translatable("item.relay.computing_core.interval", interval)
                        .withStyle(ChatFormatting.GOLD));
        textConsumer.accept(
                Component.translatable("item.relay.computing_core.tps", String.format("%.2f", tps))
                        .withStyle(ChatFormatting.GRAY));
        textConsumer.accept(
                Component.translatable("item.relay.computing_core.energy_cost", String.format("%.1f", energyCost))
                        .withStyle(ChatFormatting.RED));
        textConsumer.accept(
                Component.translatable("item.relay.computing_core.desc")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }
}
