package qdream.relay.items;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;

/**
 * 工具外壳的 MenuProvider
 * 用于手持工具外壳时打开 GUI
 */
public class ToolShellMenuProvider implements MenuProvider {

    private final ItemStack toolShell;

    public ToolShellMenuProvider(ItemStack toolShell) {
        this.toolShell = toolShell;
    }

    public ItemStack getToolShell() {
        return toolShell;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.relay.tool_shell");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        ToolShellItem toolShellItem = (ToolShellItem) toolShell.getItem();
        ToolShellContainer container = new ToolShellContainer(toolShellItem, toolShell);
        return new ToolShellScreenHandler(syncId, inv, container, toolShell);
    }
}
