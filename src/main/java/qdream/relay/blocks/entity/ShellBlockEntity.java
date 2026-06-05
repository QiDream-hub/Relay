package qdream.relay.blocks.entity;

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

import qdream.relay.blocks.RelayBlockEntities;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.McIota;
import qdream.relay.core.ShellContainer;
import qdream.relay.core.ShellTickHandler;
import qdream.relay.screen.ShellScreenHandler;

/**
 * 外壳方块实体
 * 维护状态机，执行 tick，处理持久化
 * 注意：26.1.2 的 NBT 系统有重大变化，暂时简化实现
 */
public class ShellBlockEntity extends BlockEntity implements MenuProvider, ShellContainer {

    private final ItemStack[] inventory = new ItemStack[4];
    private final StateMachine stateMachine;
    private final ShellTickHandler tickHandler;

    private int energy;

    public ShellBlockEntity(BlockPos pos, BlockState state) {
        super(RelayBlockEntities.SHELL_BLOCK_ENTITY, pos, state);
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
        this.stateMachine = new StateMachine(1024);
        this.tickHandler = new ShellTickHandler();
        this.energy = 0;

        // 设置事故回调
        this.stateMachine.setMishapHandler(reason -> {
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        });
    }

    /**
     * Tick 方法
     */
    public static void tick(Level world, BlockPos pos, BlockState state, ShellBlockEntity entity) {
        entity.tickHandler.tick(entity);
    }

    /**
     * 保存程序到磁盘
     */
    public void saveProgramToDisk() {
        ItemStack diskStack = inventory[DISK_SLOT];
        if (!diskStack.isEmpty()) {
            // TODO: 将当前状态机状态保存到磁盘
        }
    }

    // ========== MenuProvider 接口 ==========

    @Override
    public Component getDisplayName() {
        return Component.literal("外壳");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new ShellScreenHandler(syncId, inv, this);
    }

    // ========== ShellContainer 接口 ==========

    @Override
    public ItemStack getInventorySlot(int slot) {
        if (slot >= 0 && slot < inventory.length) {
            return inventory[slot];
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < inventory.length) {
            inventory[slot] = stack;
            setChanged();
        }
    }

    @Override
    public StateMachine getStateMachine() {
        return stateMachine;
    }

    @Override
    public int getCoreCount() {
        return tickHandler.getCoreCount();
    }

    @Override
    public int getInterval() {
        return tickHandler.getInterval();
    }

    @Override
    public boolean isInitialized() {
        return tickHandler.isInitialized();
    }

    @Override
    public void setInitialized(boolean initialized) {
        tickHandler.setInitialized(initialized);
    }

    @Override
    public int getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(int energy) {
        this.energy = energy;
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    // ========== 状态访问（兼容旧代码） ==========

    public int getTickCounter() {
        return tickHandler.getTickCounter();
    }

    public void setTickCounter(int tickCounter) {
        tickHandler.setTickCounter(tickCounter);
    }
}
