package qdream.relay.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

/**
 * ShellContainer 物品栏管理器
 *
 * <p>
 * 提供通用的物品栏管理、序列化逻辑
 * </p>
 *
 * <p>
 * 使用组合模式：ShellBlockEntity 和 ToolShellContainer 持有一个 ShellStateManager 实例
 * </p>
 *
 * <h3>存储结构</h3>
 *
 * <pre>
 * {
 *   "inventory": ListTag            // 4 个插槽
 * }
 * </pre>
 */
public class ShellStateManager {

    private static final int SLOT_COUNT = 4;

    protected final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public ShellStateManager() {
    }

    // ========== 数据加载/保存 ==========

    /**
     * 从 CompoundTag 加载物品栏
     */
    public void loadInventory(CompoundTag tag) {
        ListTag listTag = tag.getList("inventory").orElse(null);
        if (listTag == null) {
            return;
        }

        ItemStack[] parsed = new ItemStack[inventory.size()];
        for (int i = 0; i < Math.min(parsed.length, listTag.size()); i++) {
            Tag element = listTag.get(i);
            if (element instanceof CompoundTag compoundTag) {
                var result = ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, compoundTag);
                parsed[i] = result.result().orElse(ItemStack.EMPTY);
            }
        }

        for (int i = 0; i < parsed.length; i++) {
            if (parsed[i] != null) {
                inventory.set(i, parsed[i]);
            }
        }
    }

    /**
     * 保存物品栏到 NBT
     */
    public void saveInventory(CompoundTag tag) {
        ListTag listTag = new ListTag();

        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                ItemStack.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, stack)
                        .result()
                        .ifPresent(listTag::add);
            } else {
                listTag.add(new CompoundTag());
            }
        }

        tag.put("inventory", listTag);
    }

    // ========== 物品栏访问 ==========

    public ItemStack getInventorySlot(int slot) {
        return (slot >= 0 && slot < inventory.size()) ? inventory.get(slot) : ItemStack.EMPTY;
    }

    public void setInventorySlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < inventory.size()) {
            inventory.set(slot, stack);
        }
    }

    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }
}
