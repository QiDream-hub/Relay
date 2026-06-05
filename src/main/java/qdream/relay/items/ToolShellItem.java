package qdream.relay.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import qdream.relay.engine.StateMachine;

/**
 * 工具外壳（手持物品形态）
 * 手持右键激活程序，在物品栏中持续运行
 * 注意：26.1.2 使用 DataComponent 系统，这里暂时简化实现
 */
public class ToolShellItem extends Item {

    public ToolShellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // inventoryTick 在 26.1.2 中可能不是 @Override，暂时移除注解
    public void inventoryTick(ItemStack stack, Level world, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        // 简化实现：工具外壳暂时不自动执行
        // 完整实现需要使用 DataComponent 系统
    }

    // interact 在 26.1.2 中方法签名可能不同
    public InteractionResult interact(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide()) {
            // 右键打开 GUI
            player.openMenu(new ToolShellMenuProvider(stack, hand));
        }

        return InteractionResult.SUCCESS;
    }

    // ========== 物品栏插槽（临时实现） ==========

    public static final int CORE_SLOT = 0;
    public static final int DISK_SLOT = 1;
    public static final int ENERGY_SLOT = 2;
    public static final int INTERACTOR_SLOT = 3;

    public ItemStack getInventorySlot(ItemStack shell, int slot) {
        // TODO: 使用 DataComponent 实现
        return ItemStack.EMPTY;
    }

    public void setInventorySlot(ItemStack shell, int slot, ItemStack stack) {
        // TODO: 使用 DataComponent 实现
    }

    public ItemStack getCoreStack(ItemStack shell) {
        return getInventorySlot(shell, CORE_SLOT);
    }

    public ItemStack getDiskStack(ItemStack shell) {
        return getInventorySlot(shell, DISK_SLOT);
    }

    public ItemStack getEnergyStack(ItemStack shell) {
        return getInventorySlot(shell, ENERGY_SLOT);
    }

    public ItemStack getInteractorStack(ItemStack shell) {
        return getInventorySlot(shell, INTERACTOR_SLOT);
    }

    // ========== ToolShellContainer 需要的方法 ==========

    public StateMachine getStateMachine(ItemStack shell) {
        return new StateMachine(1024);
    }

    public boolean isInitialized(ItemStack shell) {
        return false;
    }

    public void setInitialized(ItemStack shell, boolean initialized) {
        // TODO: 使用 DataComponent 实现
    }
}
