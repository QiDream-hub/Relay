package qdream.relay.screen;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import qdream.relay.blocks.entity.custom.SpellEditorBlockEntity;
import qdream.relay.engine.Executable;
import qdream.relay.items.DiskItem;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.signature.Signature;
import qdream.relay.networking.payloads.S2C_SyncSpellDiskPayload;
import qdream.relay.mc.component.DiskComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonObject;

/**
 * 法术编辑器 Screen Handler
 * 管理编辑器的服务端状态：程序编辑、磁盘插槽、保存功能
 */
public class SpellEditorScreenHandler extends AbstractContainerMenu {

    /** 磁盘插槽索引 */
    public static final int DISK_SLOT = 0;
    /** 磁盘插槽位置 */
    private static final int DISK_SLOT_X = 160;
    private static final int DISK_SLOT_Y = 18;

    /** 背包面板起始位置 */
    private static final int INVENTORY_START_X = 124;
    private static final int INVENTORY_START_Y = 310;
    private static final int SLOT_SIZE = 18;

    /** 所有可用的操作 ID */
    private final List<String> availableOperations;

    /** 所有可用的数据类型 ID */
    private final List<String> availableDataTypes;

    /** 方块实体引用 */
    private final SpellEditorBlockEntity blockEntity;

    public SpellEditorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    public SpellEditorScreenHandler(int syncId, Inventory playerInventory, SpellEditorBlockEntity blockEntity) {
        super(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.availableOperations = new ArrayList<>(OperationRegistry.getAllOperationIds());
        this.availableDataTypes = new ArrayList<>(OperationRegistry.getAllDataIds());

        // 使用 BlockEntity 的物品栏（如果存在）
        if (blockEntity != null) {
            this.addSlot(new Slot(blockEntity, 0, DISK_SLOT_X, DISK_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof DiskItem;
                }

                @Override
                public void onQuickCraft(ItemStack stack, ItemStack previousStack) {
                    super.onQuickCraft(stack, previousStack);
                    // 磁盘放入时，加载并同步
                    if (!stack.isEmpty() && stack.getItem() instanceof DiskItem) {
                        loadProgramFromDisk(stack);
                    }
                }
                
                @Override
                public void set(ItemStack stack) {
                    super.set(stack);
                    // 磁盘变化时，加载并同步
                    if (!stack.isEmpty() && stack.getItem() instanceof DiskItem) {
                        loadProgramFromDisk(stack);
                    }
                }
            });
        } else {
            // 客户端没有 BlockEntity 时使用临时容器
            this.addSlot(new Slot(new net.minecraft.world.SimpleContainer(1), 0, DISK_SLOT_X, DISK_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof DiskItem;
                }
            });
        }

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

        // 打开 GUI 时自动从磁盘加载程序（仅当 program 为空时）
        if (blockEntity != null && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide()) {
            if (blockEntity.getProgram().isEmpty()) {
                ItemStack diskStack = getDiskItem();
                if (!diskStack.isEmpty() && diskStack.getItem() instanceof DiskItem) {
                    // 从磁盘加载并同步到客户端
                    loadProgramFromDisk(diskStack);
                }
            } else {
                // program 不为空，直接同步到客户端
                syncProgramToClient();
            }
        }
    }

    // ==================== 程序编辑 ====================

    /**
     * 从磁盘加载程序到编辑器
     * @param diskStack 磁盘物品（直接传入，避免客户端/服务端不同步）
     */
    public void loadProgramFromDisk(ItemStack diskStack) {
        if (diskStack.isEmpty()) {
            return;
        }
        DiskComponent diskComponent = getDiskComponent(diskStack);
        if (diskComponent == null) {
            return;
        }

        if (blockEntity != null) {
            List<Executable> loadedProgram = diskComponent.getProgram(diskStack);
            blockEntity.setProgram(loadedProgram);
        }

        // 同步到客户端
        syncProgramToClient();
    }

    /**
     * 从物品堆获取 SpellDiskComponent
     * @param stack 物品堆
     * @return SpellDiskComponent 实例，如果物品不是法术磁盘则返回 null
     */
    private DiskComponent getDiskComponent(ItemStack stack) {
        if (stack.getItem() instanceof DiskComponent) {
            return (DiskComponent) stack.getItem();
        }
        return null;
    }

    /**
     * 将当前程序列表同步到客户端
     */
    public void syncProgramToClient() {
        if (blockEntity != null && blockEntity.getLevel() != null) {
            Level level = blockEntity.getLevel();
            if (!level.isClientSide()) {
                level.players().stream()
                    .filter(p -> p.containerMenu == this)
                    .filter(p -> p instanceof ServerPlayer)
                    .findFirst()
                    .ifPresent(p -> {
                        try {
                            ListTag programList = ProgramCompiler.toNbt(blockEntity.getProgram());
                            CompoundTag programTag = new CompoundTag();
                            programTag.put("program", programList);
                            ServerPlayNetworking.send((ServerPlayer) p, new S2C_SyncSpellDiskPayload(programTag));
                        } catch (ProgramCompiler.CompilationException e) {
                            e.printStackTrace();
                        }
                    });
            }
        }
    }

    /**
     * 处理客户端程序修改
     * @param programTag 程序 NBT 数据
     */
    public void onProgramModified(CompoundTag programTag) {
        if (blockEntity == null) return;
        
        Optional<ListTag> listOpt = programTag.getList("program");
        if (listOpt.isEmpty()) {
            return;
        }

        try {
            List<Executable> newProgram = ProgramCompiler.fromNbt(listOpt.get());
            blockEntity.setProgram(newProgram);
        } catch (ProgramCompiler.CompilationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存程序到磁盘
     */
    public void saveProgramToDisk() {
        if (blockEntity == null) return;

        ItemStack diskStack = getDiskItem();
        if (diskStack.isEmpty()) {
            return;
        }
        DiskComponent diskComponent = getDiskComponent(diskStack);
        if (diskComponent == null) {
            return;
        }

        diskComponent.setProgram(diskStack, blockEntity.getProgram());
    }

    /**
     * 从磁盘加载程序到编辑器（使用当前插槽中的物品）
     */
    public void loadProgramFromDisk() {
        loadProgramFromDisk(getDiskItem());
    }

    public List<Executable> getProgramEntries() {
        return blockEntity != null ? blockEntity.getProgram() : new ArrayList<>();
    }

    /** 添加操作到程序 */
    public void addOperation(String opId) {
        if (blockEntity == null) return;
        if (OperationRegistry.contains(opId)) {
            JsonObject json = new JsonObject();
            json.addProperty("id", opId);
            Executable entry = ((Operation)OperationRegistry.getEntry(opId).orElse(null).create()).fromJson(json);
            blockEntity.getProgram().add(entry);
        }
    }

    /** 添加数据常量到程序 */
    public void addDataEntry(String typeId, JsonObject extraFields) {
        if (blockEntity == null) return;
        if (OperationRegistry.contains(typeId)) {
            JsonObject json = new JsonObject();
            json.addProperty("id", typeId);
            if (extraFields != null) {
                for (var entry : extraFields.entrySet()) {
                    json.add(entry.getKey(), entry.getValue());
                }
            }
            Executable entry = ((Operation)OperationRegistry.getEntry(typeId).orElse(null).create()).fromJson(json);
            blockEntity.getProgram().add(entry);
        }
    }

    /** 从程序移除条目 */
    public void removeEntry(int index) {
        if (blockEntity == null) return;
        List<Executable> program = blockEntity.getProgram();
        if (index >= 0 && index < program.size()) {
            program.remove(index);
        }
    }

    /** 清空程序 */
    public void clearProgram() {
        if (blockEntity == null) return;
        blockEntity.getProgram().clear();
    }

    // ==================== 查询 ====================

    public List<String> getAvailableOperations() {
        return availableOperations;
    }

    public List<String> getAvailableDataTypes() {
        return availableDataTypes;
    }

    public Signature getOperationSignature(String opId) {
        return OperationRegistry.getEntry(opId)
            .map(entry -> {
                Executable exec = entry.create();
                if (exec instanceof Operation op) {
                    return op.getSignature();
                }
                return null;
            })
            .orElse(null);
    }

    // ==================== 物品移动 ====================

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();

        // 从磁盘槽移出
        if (slotIndex == DISK_SLOT) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        }
        // 从玩家物品栏移到磁盘槽（只允许磁盘）
        else {
            if (stack.getItem() instanceof DiskItem) {
                if (!this.moveItemStackTo(stack, DISK_SLOT, DISK_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 非磁盘物品不允许移入磁盘槽，直接返回
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
        return true;
    }

    /**
     * 检查指定插槽是否允许放置物品
     * @param slot 插槽索引
     * @param stack 物品堆
     * @return 是否允许放置
     */
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // 磁盘插槽只允许 SpellDiskItem
        if (slot == DISK_SLOT) {
            return stack.getItem() instanceof DiskItem;
        }

        // 其他插槽（玩家物品栏）允许所有物品
        return true;
    }

    /** 获取磁盘物品 */
    public ItemStack getDiskItem() {
        return this.getSlot(DISK_SLOT).getItem();
    }
    
    /**
     * 当磁盘放入插槽时调用（由客户端通过 C2S 包触发）
     */
    public void onDiskInserted(ItemStack diskStack) {
        if (!diskStack.isEmpty() && diskStack.getItem() instanceof DiskItem) {
            loadProgramFromDisk(diskStack);
        }
    }

    /**
     * 当玩家关闭 GUI 时调用，保存程序到磁盘
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        // 关闭界面时保存程序到磁盘
        if (blockEntity != null) {
            blockEntity.saveProgramToDisk();
        }
    }
}
