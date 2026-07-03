package qdream.relay.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import qdream.relay.mc.component.ComputingCoreComponent;

/**
 * 运算核心物品基类
 * 提供操作数预算，interval 属性决定执行间隔（tick 间隔）
 * interval 值越小，执行频率越高
 *
 * @see FixedIntervalCoreItem 固定 interval 子类（当前 7 种核心）
 * @see DynamicIntervalCoreItem 动态 interval 子类（未来特殊核心）
 */
public abstract class ComputingCoreItem extends Item implements ComputingCoreComponent {

    public ComputingCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getCost(ItemStack stack) {
        return stack.getCount();
    }

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
