package qdream.relay.screen;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import qdream.relay.Relay;
import qdream.relay.core.ShellContainer;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.items.SpellDiskItem;
import qdream.relay.mc.component.EnergyModuleComponent;

/**
 * 外壳 ScreenHandler
 * 管理外壳的 4 个插槽：
 * 0 - 运算核心
 * 1 - 法术磁盘
 * 2 - 能量模块
 * 3 - 世界交互器
 */
public class ShellScreenHandler extends AbstractContainerMenu {

    // 插槽布局常量
    private static final int CONTAINER_SLOT_COUNT = 4;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 27;
    private static final int PLAYER_HOTBAR_SLOT_COUNT = 9;

    // 插槽索引范围
    private static final int CONTAINER_START = 0;
    private static final int CONTAINER_END = CONTAINER_SLOT_COUNT;
    private static final int INVENTORY_START = CONTAINER_END;
    private static final int INVENTORY_END = INVENTORY_START + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int HOTBAR_START = INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + PLAYER_HOTBAR_SLOT_COUNT;

    // GUI 布局
    private static final int CONTAINER_SLOT_X = 50; // 向左移动，靠近标签
    private static final int CONTAINER_SLOT_Y = 12;
    private static final int SLOT_SPACING_Y = 30;
    private static final int INVENTORY_SLOT_X = 8;
    private static final int INVENTORY_SLOT_Y = 140;
    private static final int HOTBAR_SLOT_Y = 198;
    private static final int SLOT_SIZE = 18;

    private final ShellContainer container;
    private final Container wrapper;

    // 数据同步槽（服务端 → 客户端）
    private final DataSlot enabledSlot = DataSlot.standalone();
    private final DataSlot coreCountSlot = DataSlot.standalone();
    private final DataSlot initializedSlot = DataSlot.standalone();
    
    // 能量值通过网络包同步（不使用 DataSlot，因为 DataSlot 只同步 16 位）
    private double syncedEnergy = 0.0;

    /**
     * 客户端构造方法（没有实际容器）
     */
    public ShellScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    /**
     * 服务端构造方法（有实际容器）
     */
    public ShellScreenHandler(int syncId, Inventory playerInventory, ShellContainer container) {
        super(RelayScreenHandlers.SHELL_SCREEN_HANDLER, syncId);
        this.container = container;
        // ShellBlockEntity 已实现 Container，直接使用；客户端使用空容器
        this.wrapper = container != null ? (Container) container : new EmptyShellContainer();

        checkContainerSize(this.wrapper, CONTAINER_SLOT_COUNT);

        // 注册数据同步槽
        this.addDataSlot(enabledSlot);
        this.addDataSlot(coreCountSlot);
        this.addDataSlot(initializedSlot);

        // 初始化同步槽的值（确保 GUI 打开时立即显示正确状态）
        if (container != null) {
            enabledSlot.set(container.isEnabled() ? 1 : 0);
            coreCountSlot.set(container.getCoreCount());
            initializedSlot.set(container.isInitialized() ? 1 : 0);
            syncedEnergy = container.getEnergy();
        }

        // 外壳 4 个插槽（垂直排列）
        for (int i = 0; i < CONTAINER_SLOT_COUNT; ++i) {
            final int slotIndex = i;
            this.addSlot(new Slot(this.wrapper, slotIndex, CONTAINER_SLOT_X, CONTAINER_SLOT_Y + i * SLOT_SPACING_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return canPlaceItem(slotIndex, stack);
                }
            });
        }

        // 玩家主物品栏（3 行 x9 列）
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, INVENTORY_SLOT_X + x * SLOT_SIZE,
                        INVENTORY_SLOT_Y + y * SLOT_SIZE));
            }
        }

        // 玩家热键栏（1 行 x9 列）
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, INVENTORY_SLOT_X + x * SLOT_SIZE, HOTBAR_SLOT_Y));
        }
    }

    /**
     * Shift+点击物品时的快速移动逻辑
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            clicked = stackInSlot.copy();

            // 从容器插槽移动到玩家物品栏
            if (slotIndex < CONTAINER_END) {
                if (!this.moveItemStackTo(stackInSlot, INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 从玩家物品栏移动到容器插槽
            else if (slotIndex < HOTBAR_END) {
                if (!this.moveItemStackTo(stackInSlot, CONTAINER_START, CONTAINER_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            // 更新插槽状态
            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.wrapper.stillValid(player);
    }

    /**
     * 根据插槽类型限制可放置的物品
     * @param slot 插槽索引 (0-核心，1-磁盘，2-能量模块，3-世界交互器)
     * @param stack 物品堆
     * @return 是否允许放置
     */
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        return switch (slot) {
            case ShellContainer.CORE_SLOT -> item instanceof ComputingCoreComponent;
            case ShellContainer.DISK_SLOT -> item instanceof SpellDiskItem;
            case ShellContainer.ENERGY_SLOT -> item instanceof EnergyModuleComponent;
            case ShellContainer.INTERACTOR_SLOT -> true; // 世界交互器插槽允许任意物品
            default -> false;
        };
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // 从服务端同步状态到客户端
        if (container != null) {
            enabledSlot.set(container.isEnabled() ? 1 : 0);
            coreCountSlot.set(container.getCoreCount());
            initializedSlot.set(container.isInitialized() ? 1 : 0);
        }
    }

    /**
     * 客户端调用：切换开关状态（本地预览）
     * 实际服务端切换通过网络包完成
     */
    public void toggleEnabled() {
        enabledSlot.set(enabledSlot.get() == 0 ? 1 : 0);
    }

    /** 获取当前启用状态（读取同步槽） */
    public boolean isEnabled() {
        return enabledSlot.get() != 0;
    }

    /** 获取同步的核心数量 */
    public int getSyncedCoreCount() {
        return coreCountSlot.get();
    }

    /** 获取同步的能量值 */
    public double getSyncedEnergy() {
        return syncedEnergy;
    }

    /** 设置同步的能量值（通过网络包接收） */
    public void setSyncedEnergy(double energy) {
        this.syncedEnergy = energy;
    }

    /** 获取同步的初始化状态 */
    public boolean isSyncedInitialized() {
        return initializedSlot.get() != 0;
    }

    /**
     * 获取实际的 ShellContainer（可能为 null）
     */
    public ShellContainer getContainer() {
        return container;
    }

    /**
     * 空容器实现（用于客户端）
     * 持有独立的物品数组，Minecraft 会通过同步包更新内容
     */
    private static class EmptyShellContainer implements Container {
        private final ItemStack[] items = new ItemStack[CONTAINER_SLOT_COUNT];

        public EmptyShellContainer() {
            // 初始化所有插槽为空
            for (int i = 0; i < CONTAINER_SLOT_COUNT; i++) {
                items[i] = ItemStack.EMPTY;
            }
        }

        @Override
        public int getContainerSize() {
            return CONTAINER_SLOT_COUNT;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < CONTAINER_SLOT_COUNT ? items[slot] : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                ItemStack result = stack.split(amount);
                if (!result.isEmpty()) {
                    setItem(slot, stack);
                }
                return result;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                items[slot] = ItemStack.EMPTY;
                return stack;
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot >= 0 && slot < CONTAINER_SLOT_COUNT) {
                items[slot] = stack;
            }
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < CONTAINER_SLOT_COUNT; i++) {
                items[i] = ItemStack.EMPTY;
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void setChanged() {
        }
    }

}
