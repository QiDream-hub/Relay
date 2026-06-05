package qdream.relay.screen;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import qdream.relay.screen.RelayScreenHandlers;
import qdream.relay.core.ShellContainer;
import qdream.relay.items.ShellContainerWrapper;

/**
 * 外壳 ScreenHandler
 * 管理外壳的 4 个插槽：
 * 0 - 运算核心
 * 1 - 法术磁盘
 * 2 - 能量模块
 * 3 - 世界交互器
 */
public class ShellScreenHandler extends AbstractContainerMenu {

    private final ShellContainer container;

    public ShellScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    public ShellScreenHandler(int syncId, Inventory playerInventory, ShellContainer container) {
        super(RelayScreenHandlers.SHELL_SCREEN_HANDLER, syncId);
        this.container = container;

        // 外壳 4 个插槽
        for (int i = 0; i < 4; ++i) {
            final int slotIndex = i;
            this.addSlot(new Slot(new ShellContainerWrapper(container), slotIndex, 80, 10 + i * 30) {
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
