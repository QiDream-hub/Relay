package qdream.relay.screen;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import qdream.relay.blocks.entity.custom.BlockShellEntity;
import qdream.relay.core.ShellContainer;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.items.DiskItem;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.mc.component.WorldInteractorComponent;

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

    private final BlockShellEntity blockEntity;
    private final Container wrapper;

    // 数据同步槽（服务端 → 客户端）
    private final DataSlot enabledSlot = DataSlot.standalone();
    private final DataSlot coreCostSlot = DataSlot.standalone();
    private final DataSlot localCoreCostSlot = DataSlot.standalone(); // 本地核心数量
    private final DataSlot initializedSlot = DataSlot.standalone();
    private final DataSlot energyCostSlot = DataSlot.standalone(); // 能量消耗（整数部分）
    private final DataSlot energyCostFracSlot = DataSlot.standalone(); // 能量消耗（小数部分*1000）

    // 能量值通过网络包同步（不使用 DataSlot，因为 DataSlot 只同步 16 位）
    private double syncedEnergy = 0.0;

    // 日志变更标记
    private boolean logsChanged = false;
    private java.util.List<String> syncedLogs = new java.util.ArrayList<>();

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
        this.blockEntity = container instanceof BlockShellEntity be ? be : null;
        // ShellBlockEntity 已实现 Container，直接使用；客户端使用空容器
        this.wrapper = blockEntity != null ? (Container) blockEntity : new EmptyShellContainer();

        checkContainerSize(this.wrapper, CONTAINER_SLOT_COUNT);

        // 注册数据同步槽
        this.addDataSlot(enabledSlot);
        this.addDataSlot(coreCostSlot);
        this.addDataSlot(localCoreCostSlot);
        this.addDataSlot(initializedSlot);
        this.addDataSlot(energyCostSlot);
        this.addDataSlot(energyCostFracSlot);

        // 初始化同步槽的值（确保 GUI 打开时立即显示正确状态）
        if (blockEntity != null) {
            enabledSlot.set(blockEntity.isEnabled() ? 1 : 0);
            coreCostSlot.set(blockEntity.getCoreCost());
            localCoreCostSlot.set(getLocalCoreCost());
            double energyCost = blockEntity.getEnergyCostPerTick();
            energyCostSlot.set((int) energyCost);
            energyCostFracSlot.set((int) ((energyCost % 1) * 1000));
            syncedEnergy = blockEntity.getEnergy();
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
     * 
     * @param slot  插槽索引 (0-核心，1-磁盘，2-能量模块，3-世界交互器)
     * @param stack 物品堆
     * @return 是否允许放置
     */
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        return switch (slot) {
            case BlockShellEntity.CORE_SLOT -> item instanceof ComputingCoreComponent;
            case BlockShellEntity.DISK_SLOT -> item instanceof DiskItem;
            case BlockShellEntity.ENERGY_SLOT -> item instanceof EnergyModuleComponent;
            case BlockShellEntity.INTERACTOR_SLOT -> item instanceof WorldInteractorComponent; // 世界交互器插槽允许任意物品
            default -> false;
        };
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // 从服务端同步状态到客户端
        if (blockEntity != null) {
            enabledSlot.set(blockEntity.isEnabled() ? 1 : 0);
            initializedSlot.set(blockEntity.isInitialized() ? 1 : 0); // 同步 initialized 状态
            coreCostSlot.set(blockEntity.getCoreCost());
            localCoreCostSlot.set(getLocalCoreCost());
            double energyCost = blockEntity.getEnergyCostPerTick();
            energyCostSlot.set((int) energyCost);
            energyCostFracSlot.set((int) ((energyCost % 1) * 1000));

            // 同步日志（每 tick 检查变更）
            java.util.List<String> currentLogs = blockEntity.getLogBuffer();
            if (!currentLogs.equals(syncedLogs)) {
                syncedLogs = currentLogs;
                logsChanged = true;
            }
        }
    }

    /**
     * 获取同步的日志内容
     */
    public java.util.List<String> getSyncedLogs() {
        return syncedLogs;
    }

    /**
     * 设置同步的日志内容（客户端调用）
     */
    public void setSyncedLogs(java.util.List<String> logs) {
        this.syncedLogs = logs;
    }

    /**
     * 标记日志已同步（客户端调用）
     */
    public void markLogsSynced() {
        this.logsChanged = false;
    }

    /**
     * 检查日志是否有变更（服务端调用）
     */
    public boolean hasLogsChanged() {
        return logsChanged;
    }

    /**
     * 获取本地核心数量（当前方块的核心插槽中的核心数量）
     */
    private int getLocalCoreCost() {
        if (blockEntity == null) {
            return 0;
        }
        ItemStack coreStack = blockEntity.getCoreStack();
        return !coreStack.isEmpty() ? coreStack.getCount() : 0;
    }

    /**
     * 获取本地核心数量
     */
    public int getLocalCoreCount() {
        return localCoreCostSlot.get();
    }

    /** 获取当前启用状态（读取同步槽） */
    public boolean isEnabled() {
        return enabledSlot.get() != 0;
    }

    /** 获取同步的核心数量 */
    public int getSyncedCoreCount() {
        return coreCostSlot.get();
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

    /** 获取同步的能量消耗（每 tick） */
    public double getSyncedEnergyCost() {
        int intPart = energyCostSlot.get();
        int fracPart = energyCostFracSlot.get();
        return intPart + (fracPart / 1000.0);
    }

    /**
     * 获取 BlockShellEntity（仅服务端有效，客户端返回 null）
     */
    public BlockShellEntity getBlockEntity() {
        return blockEntity;
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
