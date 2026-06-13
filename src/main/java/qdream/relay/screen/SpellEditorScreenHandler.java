package qdream.relay.screen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import qdream.relay.blocks.entity.SpellEditorBlockEntity;
import qdream.relay.engine.Executable;
import qdream.relay.items.SpellDiskItem;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.OperationSignature;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;
import qdream.relay.mc.base.Operation;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

/**
 * 法术编辑器 Screen Handler
 * 管理编辑器的服务端状态：程序编辑、磁盘插槽、保存功能
 */
public class SpellEditorScreenHandler extends AbstractContainerMenu {

    /** 磁盘插槽索引 */
    public static final int DISK_SLOT = 0;
    /** 磁盘插槽位置 */
    private static final int DISK_SLOT_X = 260;
    private static final int DISK_SLOT_Y = 18;
    
    /** 背包面板起始位置（与 InventoryPanelWidget 保持一致） */
    private static final int INVENTORY_START_X = 124; // (410 - 178) / 2 + 8
    private static final int INVENTORY_START_Y = 310;
    private static final int SLOT_SIZE = 18;

    /**
     * 当前编辑的程序条目列表
     * json格式
     */
    private final List<Executable> program;

    /** 所有可用的操作 ID */
    private final List<String> availableOperations;

    /** 所有可用的数据类型 ID */
    private final List<String> availableDataTypes;

    /** 方块实体引用 */
    private final SpellEditorBlockEntity blockEntity;

    /** 磁盘容器 */
    private final SimpleContainer diskContainer;

    public SpellEditorScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    public SpellEditorScreenHandler(int syncId, Inventory playerInventory, SpellEditorBlockEntity blockEntity) {
        super(RelayScreenHandlers.SPELL_EDITOR_SCREEN_HANDLER, syncId);
        this.blockEntity = blockEntity;
        this.program = new ArrayList<>();
        this.availableOperations = new ArrayList<>(OperationRegistry.getAllOperationIds());
        this.availableDataTypes = new ArrayList<>(OperationRegistry.getAllDataIds());

        // 磁盘容器 + 插槽
        this.diskContainer = new SimpleContainer(1) {
            @Override
            public void setChanged() {
                super.setChanged();
            }
        };
        this.addSlot(new Slot(this.diskContainer, 0, DISK_SLOT_X, DISK_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SpellDiskItem;
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
        int hotbarY = INVENTORY_START_Y + 54; // 3 行主物品栏下方的额外偏移
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, 
                INVENTORY_START_X + x * SLOT_SIZE, 
                hotbarY));
        }
    }

    // ==================== 程序编辑 ====================

    public List<Executable> getProgramEntries() {
        return program;
    }

    /** 添加操作到程序 */
    public void addOperation(String opId) {
        if (OperationRegistry.contains(opId)) {
            JsonObject json = new JsonObject();
            json.addProperty("id", opId);
            this.program.add(((Operation)OperationRegistry.getEntry(opId).orElse(null).create()).fromJson(json));
        }
    }

    /** 添加数据常量到程序 */
    public void addDataEntry(String typeId, JsonObject extraFields) {
        if (OperationRegistry.contains(typeId)) {
            JsonObject json = new JsonObject();
            json.addProperty("id", typeId);
            // 合并额外字段到 JSON 中
            if (extraFields != null) {
                for (var entry : extraFields.entrySet()) {
                    json.add(entry.getKey(), entry.getValue());
                }
            }
            this.program.add(((Operation)OperationRegistry.getEntry(typeId).orElse(null).create()).fromJson(json));
        }
    }

    /** 从程序移除条目 */
    public void removeEntry(int index) {
        if (index >= 0 && index < this.program.size()) {
            this.program.remove(index);
        }
    }

    /** 清空程序 */
    public void clearProgram() {
        this.program.clear();
    }

    // ==================== 查询 ====================

    public List<String> getAvailableOperations() {
        return availableOperations;
    }

    public List<String> getAvailableDataTypes() {
        return availableDataTypes;
    }

    public OperationSignature getOperationSignature(String opId) {
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

    // ==================== 保存 ====================

    /**
     * 将当前程序保存到磁盘插槽中的法术磁盘
     * @return 保存结果消息
     * @throws CompilationException 
     */
    public String saveToDisk() throws CompilationException {
        ItemStack diskStack = getDiskItem();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof SpellDiskItem)) {
            return "请放入法术磁盘";
        }
        if (this.program.isEmpty()) {
            return "程序为空";
        }

        SpellDiskItem.setProgram(diskStack, this.program);
        return "已保存 " + this.program.size() + " 条指令";
    }

    /** 将程序序列化为 CompoundTag（用于网络传输） */
    public CompoundTag programToTag() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag;
        try {
            listTag = ProgramCompiler.toNbt(this.program);
        } catch (CompilationException e) {
            listTag = new ListTag();
            e.printStackTrace();
        }
        tag.put("program", listTag);
        return tag;
    }

    /** 从 CompoundTag 反序列化程序（用于网络传输） */
    public void programFromTag(CompoundTag tag) {
        tag.getList("program").ifPresent(listTag -> {
            try {
                this.program.addAll(ProgramCompiler.fromNbt(listTag));
            } catch (CompilationException e) {
                e.printStackTrace();
            }
        });
    }

    /** 获取磁盘物品 */
    public ItemStack getDiskItem() {
        return this.getSlot(DISK_SLOT).getItem();
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
        // 从玩家物品栏移到磁盘槽
        else {
            if (stack.getItem() instanceof SpellDiskItem) {
                if (!this.moveItemStackTo(stack, DISK_SLOT, DISK_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
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

    /** 获取磁盘容器（供客户端检测变化） */
    public SimpleContainer getDiskContainer() {
        return diskContainer;
    }
}
