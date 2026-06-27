package qdream.relay.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import qdream.relay.Component.RelayDataComponents;
import qdream.relay.core.EnergySystem;

/**
 * 能量模块物品
 * Shift+右键自动充能背包内的紫水晶，右键显示当前能量
 */
public class EnergyModuleItem extends Item {

    public EnergyModuleItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 获取当前存储的能量
     */
    public static double getStoredEnergy(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EnergyModuleItem)) {
            return 0;
        }
        Double energy = stack.get(RelayDataComponents.ENERGY);
        return energy != null ? energy : 0.0;
    }

    /**
     * 设置能量值
     */
    public static void setStoredEnergy(ItemStack stack, double energy) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EnergyModuleItem)) {
            return;
        }
        stack.set(RelayDataComponents.ENERGY, Math.max(0, energy));
    }

    /**
     * 添加能量
     * 
     * @return 实际添加的能量值
     */
    public static double addEnergy(ItemStack stack, double amount) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EnergyModuleItem)) {
            return 0;
        }
        double current = getStoredEnergy(stack);
        double newEnergy = current + amount;
        setStoredEnergy(stack, newEnergy);
        return amount;
    }

    /**
     * 消耗能量
     * 
     * @return 实际消耗的能量值
     */
    public static double consumeEnergy(ItemStack stack, double amount) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EnergyModuleItem)) {
            return 0;
        }
        double current = getStoredEnergy(stack);
        double toConsume = Math.min(amount, current);
        if (toConsume > 0) {
            setStoredEnergy(stack, current - toConsume);
        }
        return toConsume;
    }

    /**
     * 检查是否有足够能量
     */
    public static boolean hasEnergy(ItemStack stack, double amount) {
        return getStoredEnergy(stack) >= amount;
    }

    /**
     * Shift+右键自动充能背包内的紫水晶
     */
    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide()) {
            if (player.isShiftKeyDown()) {
                // Shift+右键：扫描背包自动充能
                double addEnergy = chargeFromInventory(player, stack);
                if (addEnergy > 0) {
                    player.sendSystemMessage(
                            Component.literal("§a已充能 §e" + addEnergy + " §a，当前能量：§e"
                                    + String.format("%.1f", getStoredEnergy(stack))));
                } else {
                    player.sendSystemMessage(
                            Component.literal("§7背包内没有可充能的紫水晶"));
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 从玩家物品栏中吸收紫水晶能量
     * 
     * @return 充能的紫水晶数量
     */
    public static double chargeFromInventory(Player player, ItemStack module) {
        if (module.isEmpty() || !(module.getItem() instanceof EnergyModuleItem)) {
            return 0;
        }

        Inventory inv = player.getInventory();
        double addEnergyCount = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.isEmpty()) {
                continue;
            }

            // 确保有能量
            if (!EnergySystem.hasEnergy(slot)) {
                continue;
            }

            double energyValue = EnergySystem.getEnergyValue(slot);
            addEnergy(module, energyValue);
            addEnergyCount += energyValue;

            // 移除物品
            inv.removeItem(i, slot.count());
        }

        return addEnergyCount;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent,
            Consumer<Component> textConsumer, TooltipFlag type) {
        double energy = getStoredEnergy(stack);
        textConsumer.accept(
                Component.translatable("item.relay.energy_module.energy", String.format("%.1f", energy))
                        .withStyle(ChatFormatting.GOLD));
        textConsumer.accept(
                Component.translatable("item.relay.energy_module.charge").withStyle(ChatFormatting.GRAY));
    }
}
