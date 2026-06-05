package qdream.relay.items;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

/**
 * 工具外壳的 MenuProvider
 */
public class ToolShellMenuProvider implements MenuProvider {

    private final ItemStack toolShell;
    private final InteractionHand hand;

    public ToolShellMenuProvider(ItemStack toolShell, InteractionHand hand) {
        this.toolShell = toolShell;
        this.hand = hand;
    }

    public ItemStack getToolShell() {
        return toolShell;
    }

    public InteractionHand getHand() {
        return hand;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("工具外壳");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        // TODO: 返回 ShellScreenHandler
        return null;
    }
}
