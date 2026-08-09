package qdream.relay.blocks.entity.custom;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.screen.EditorScreenHandler;

/**
 * 法术编辑器方块实体
 * 实现 MenuProvider 接口以支持 GUI 打开
 * 使用 ValueInput/ValueOutput 处理 26.1.2 的序列化系统
 */
public class EditorBlockEntity extends BlockEntity implements MenuProvider, Container {

    /** 物品栏：1 个插槽用于法术磁盘 */
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    public EditorBlockEntity(BlockPos pos, BlockState state) {
        super(RelayBlockEntities.SPELL_EDITOR_BLOCK_ENTITY, pos, state);
    }

    // ========== Container 接口 ==========

    @Override
    public int getContainerSize() {
        return inventory.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getInventorySlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot >= 0 && slot < inventory.size()) {
            ItemStack stack = inventory.get(slot);
            if (!stack.isEmpty()) {
                ItemStack result = stack.split(amount);
                setChanged();
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot >= 0 && slot < inventory.size()) {
            ItemStack stack = inventory.get(slot);
            if (!stack.isEmpty()) {
                inventory.set(slot, ItemStack.EMPTY);
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setInventorySlot(slot, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof qdream.relay.items.DiskItem;
    }

    @Override
    public void clearContent() {
        inventory.clear();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level != null && this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return true;
    }

    // ========== 物品栏访问 ==========

    /** 获取磁盘物品 */
    public ItemStack getDiskStack() {
        return getInventorySlot(0);
    }

    /** 设置磁盘物品 */
    public void setDiskStack(ItemStack stack) {
        setInventorySlot(0, stack);
    }

    private ItemStack getInventorySlot(int slot) {
        if (slot >= 0 && slot < inventory.size()) {
            return inventory.get(slot);
        }
        return ItemStack.EMPTY;
    }

    private void setInventorySlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < inventory.size()) {
            inventory.set(slot, stack);
            setChanged();
        }
    }

    // ========== MenuProvider 接口 ==========

    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new EditorScreenHandler(syncId, inv, this);
    }

    // ========== NBT 序列化与反序列化 (26.1.2 ValueInput/ValueOutput) ==========

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // 保存物品栏
        ContainerHelper.saveAllItems(output, this.inventory);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // 加载物品栏
        ContainerHelper.loadAllItems(input, this.inventory);
    }

    // ========== 网络同步 ==========

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }
}
