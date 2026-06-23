package qdream.relay.blocks.entity.custom;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.engine.Executable;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.screen.SpellEditorScreenHandler;

/**
 * 法术编辑器方块实体
 * 实现 MenuProvider 接口以支持 GUI 打开
 * 使用 ValueInput/ValueOutput 处理 26.1.2 的序列化系统
 */
public class SpellEditorBlockEntity extends BlockEntity implements MenuProvider, Container {

    /** 物品栏：1 个插槽用于法术磁盘 */
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(1, ItemStack.EMPTY);

    /** 程序列表：持久化存储，不随 GUI 关闭而丢失 */
    private final List<Executable> program = new ArrayList<>();

    public SpellEditorBlockEntity(BlockPos pos, BlockState state) {
        super(RelayBlockEntities.SPELL_EDITOR_BLOCK_ENTITY, pos, state);
    }

    // ========== 程序列表访问 ==========

    /**
     * 获取程序列表
     */
    public List<Executable> getProgram() {
        return program;
    }

    /**
     * 设置程序列表
     */
    public void setProgram(List<Executable> program) {
        this.program.clear();
        this.program.addAll(program);
        setChanged();
    }

    /**
     * 从磁盘加载程序
     */
    public void loadProgramFromDisk() {
        ItemStack diskStack = getDiskStack();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof qdream.relay.items.SpellDiskItem)) {
            return;
        }
        List<Executable> loadedProgram = qdream.relay.items.SpellDiskItem.getProgram(diskStack);
        setProgram(loadedProgram);
    }

    /**
     * 保存程序到磁盘
     */
    public void saveProgramToDisk() {
        ItemStack diskStack = getDiskStack();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof qdream.relay.items.SpellDiskItem)) {
            return;
        }
        qdream.relay.items.SpellDiskItem.setProgram(diskStack, this.program);
        setChanged();
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
        return slot == 0 && stack.getItem() instanceof qdream.relay.items.SpellDiskItem;
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
        return new SpellEditorScreenHandler(syncId, inv, this);
    }

    // ========== NBT 序列化与反序列化 (26.1.2 ValueInput/ValueOutput) ==========

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // 保存物品栏 - 使用 ContainerHelper 处理 DataComponent 系统
        ContainerHelper.saveAllItems(output, this.inventory);

        // 保存程序列表 - 使用 CompoundTag.CODEC 序列化
        try {
            ListTag programTag = ProgramCompiler.toNbt(this.program);
            CompoundTag nbt = new CompoundTag();
            nbt.put("program", programTag);
            output.store("editorProgram", CompoundTag.CODEC, nbt);
        } catch (ProgramCompiler.CompilationException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // 加载物品栏 - 使用 ContainerHelper 处理 DataComponent 系统
        ContainerHelper.loadAllItems(input, this.inventory);

        // 加载程序列表
        input.read("editorProgram", CompoundTag.CODEC).ifPresent(nbt -> {
            try {
                ListTag listTag = ((CompoundTag) nbt).getList("program").orElse(null);
                if (listTag != null) {
                    List<Executable> loaded = ProgramCompiler.fromNbt(listTag);
                    this.program.clear();
                    this.program.addAll(loaded);
                }
            } catch (ProgramCompiler.CompilationException e) {
                e.printStackTrace();
            }
        });
    }
}
