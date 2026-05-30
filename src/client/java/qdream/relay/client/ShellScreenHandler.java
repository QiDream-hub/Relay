package qdream.relay.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 外壳 ScreenHandler
 */
public class ShellScreenHandler extends AbstractContainerMenu {

    public ShellScreenHandler(int syncId, Inventory playerInventory) {
        super(RelayScreenHandlers.SHELL_SCREEN_HANDLER, syncId);

        // 添加 4 个插槽
        for (int i = 0; i < 4; ++i) {
            final int index = i;
            this.addSlot(new Slot(playerInventory, i, 80, 10 + i * 30) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true;
                }
            });
        }

        // 玩家物品栏
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 140 + y * 18));
            }
        }

        // 玩家热键栏
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 198));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int invSlot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
