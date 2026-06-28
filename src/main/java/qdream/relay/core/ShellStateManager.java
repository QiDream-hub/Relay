package qdream.relay.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import qdream.relay.engine.StateMachine;
import qdream.relay.mc.StateMachineNbtSerializer;

/**
 * ShellContainer 状态管理器
 * 
 * <p>提供通用的物品栏管理、StateMachine 持久化、Owner 管理逻辑</p>
 * 
 * <p>使用组合模式：ShellBlockEntity 和 ToolShellContainer 持有一个 ShellStateManager 实例</p>
 * 
 * <h3>存储结构</h3>
 * <pre>
 * {
 *   "inventory": ListTag,           // 4 个插槽
 *   "stateMachine": {...},          // StateMachine NBT
 *   "owner": "uuid-string"          // 拥有者 UUID (可选)
 * }
 * </pre>
 */
public class ShellStateManager {

    private static final int SLOT_COUNT = 4;

    protected final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    protected final StateMachine stateMachine = new StateMachine(1024);
    protected Entity owner;
    private java.util.UUID ownerUuid;

    public void setOwnerUuid(java.util.UUID uuid) {
        this.ownerUuid = uuid;
    }

    public java.util.UUID getOwnerUuid() {
        return ownerUuid;
    }

    public ShellStateManager() {
    }

    // ========== 数据加载/保存 ==========

    /**
     * 从 CompoundTag 加载所有状态
     */
    public void loadFromTag(CompoundTag data) {
        if (data == null) {
            return;
        }

        // 加载物品栏
        loadInventory(data, inventory);

        // 加载 StateMachine
        CompoundTag machineTag = data.getCompound("stateMachine").orElse(null);
        if (machineTag != null) {
            StateMachineNbtSerializer.INSTANCE.deserialize(stateMachine, machineTag);
        }

        // 加载 Owner
        String uuidStr = data.getString("owner").orElse("");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUuid = java.util.UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                // UUID 格式错误，忽略
            }
        }
    }

    /**
     * 保存所有状态到 CompoundTag
     */
    public CompoundTag saveToTag() {
        CompoundTag data = new CompoundTag();

        // 保存物品栏
        saveInventory(data, inventory);

        // 保存 StateMachine
        CompoundTag machineTag = StateMachineNbtSerializer.INSTANCE.serialize(stateMachine);
        data.put("stateMachine", machineTag);

        // 保存 Owner
        if (owner != null) {
            data.putString("owner", owner.getUUID().toString());
        }

        return data;
    }

    /**
     * 从 NBT 加载物品栏
     */
    public void loadInventory(CompoundTag tag, NonNullList<ItemStack> inventory) {
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
    public void saveInventory(CompoundTag tag, NonNullList<ItemStack> inventory) {
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

    // ========== StateMachine 访问 ==========

    public StateMachine getStateMachine() {
        return stateMachine;
    }

    // ========== Owner 管理 ==========

    public Entity getOwner() {
        return owner;
    }

    public void setOwner(Entity owner) {
        this.owner = owner;
        if (owner != null) {
            this.ownerUuid = owner.getUUID();
        }
    }

    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }
}
