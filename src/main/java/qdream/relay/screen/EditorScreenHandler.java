package qdream.relay.screen;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import qdream.relay.blocks.entity.custom.EditorBlockEntity;
import qdream.relay.networking.payloads.S2C_SyncSpellDiskPayload;
import qdream.relay.mc.component.DiskComponent;
import qdream.relay.items.DiskItem;

/**
 * 法术编辑器 Screen Handler
 * 管理编辑器的服务端状态：程序编辑、磁盘插槽、保存功能
 */
public class EditorScreenHandler extends AbstractContainerMenu {

    /** 插槽数量 */
    private static final int CONTAINER_SLOT_COUNT = 1;

    /** 磁盘插槽索引 */
    public static final int DISK_SLOT = 0;

    /** 磁盘插槽位置 */
    private static final int DISK_SLOT_X = 160;
    private static final int DISK_SLOT_Y = 18;

    /** 背包面板起始位置 */
    private static final int INVENTORY_START_X = 124;
    private static final int INVENTORY_START_Y = 310;
    private static final int SLOT_SIZE = 18;

    /** 方块实体引用 */
    private final EditorBlockEntity blockEntity;

    /** 容器包装器（服务端为 BlockEntity，客户端为空容器） */
    private final Container wrapper;

    public EditorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    public EditorScreenHandler(int syncId, Inventory playerInventory, EditorBlockEntity blockEntity) {
        super(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.wrapper = blockEntity != null ? blockEntity : new SimpleContainer(CONTAINER_SLOT_COUNT);

        checkContainerSize(this.wrapper, CONTAINER_SLOT_COUNT);

        // 磁盘插槽
        this.addSlot(new Slot(this.wrapper, DISK_SLOT, DISK_SLOT_X, DISK_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return canPlaceItem(DISK_SLOT, stack);
            }
        });

        // 玩家物品栏（主物品栏 3 行 x 9 列）
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9,
                        INVENTORY_START_X + x * SLOT_SIZE,
                        INVENTORY_START_Y + y * SLOT_SIZE));
            }
        }

        // 玩家热键栏（1 行 x 9 列）
        int hotbarY = INVENTORY_START_Y + 54;
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x,
                    INVENTORY_START_X + x * SLOT_SIZE,
                    hotbarY));
        }

        // 打开 GUI 时自动从磁盘加载程序（仅服务端）
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
            ItemStack diskStack = getDiskItem();
            if (!diskStack.isEmpty() && diskStack.getItem() instanceof DiskItem) {
                // 从磁盘加载并同步到客户端
                loadProgramFromDisk(diskStack);
            }
        }
    }

    // ==================== 物品移动 ====================

    /**
     * 根据插槽类型限制可放置的物品
     *
     * @param slot  插槽索引
     * @param stack 物品堆
     * @return 是否允许放置
     */
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        return switch (slot) {
            case DISK_SLOT -> item instanceof DiskItem;
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();

        // 从磁盘槽移出
        if (slotIndex == DISK_SLOT) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 从玩家物品栏移到磁盘槽（只允许磁盘）
            if (!this.moveItemStackTo(stack, DISK_SLOT, DISK_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.wrapper.stillValid(player);
    }

    // ==================== 程序编辑 ====================

    /**
     * 从磁盘加载程序到编辑器
     *
     * @param diskStack 磁盘物品（直接传入，避免客户端/服务端不同步）
     */
    public void loadProgramFromDisk(ItemStack diskStack) {
        if (diskStack.isEmpty()) {
            return;
        }

        if (!(diskStack.getItem() instanceof DiskComponent diskComponent)) {
            return;
        }

        // 同步到客户端（JSON 字符串）
        String programJson = diskComponent.getProgram(diskStack);
        if (programJson != null) {
            syncProgramToClient(programJson);
        }
    }

    /**
     * 将程序同步到客户端（JSON 字符串）
     */
    public void syncProgramToClient(String programJson) {
        if (blockEntity != null && blockEntity.getLevel() != null) {
            Level level = blockEntity.getLevel();
            if (!level.isClientSide()) {
                level.players().stream()
                        .filter(p -> p.containerMenu == this)
                        .filter(p -> p instanceof ServerPlayer)
                        .findFirst()
                        .ifPresent(p -> ServerPlayNetworking.send((ServerPlayer) p, new S2C_SyncSpellDiskPayload(programJson)));
            }
        }
    }

    /**
     * 处理客户端程序修改（已移除，不再需要）
     * 客户端直接编辑 JSON，保存时发送到服务端
     */

    /**
     * 保存程序到磁盘
     *
     * @param programTag 程序 NBT 数据（向后兼容，不再使用）
     */
    public void saveProgramToDisk(CompoundTag programTag) {
        // 向后兼容：此方法不再使用
        // 新流程由服务端网络接收器直接编译 JSON 并调用 DiskComponent.setProgramJson()
    }

    /** 获取磁盘物品 */
    public ItemStack getDiskItem() {
        return this.getSlot(DISK_SLOT).getItem();
    }

    /**
     * 当客户端请求加载磁盘时调用（由 C2S_DiskInsertedPayload 触发）
     */
    public void onDiskInserted() {
        ItemStack diskStack = getDiskItem();
        if (!diskStack.isEmpty() && diskStack.getItem() instanceof DiskItem) {
            loadProgramFromDisk(diskStack);
        }
    }

}
