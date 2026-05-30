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

/**
 * 外壳方块实体
 * 维护状态机，执行 tick，处理持久化
 */
public class ShellBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStack[] inventory = new ItemStack[4];

    private int coreCount;
    private int interval;
    private int tickCounter;
    private boolean initialized;

    // 插槽索引
    public static final int CORE_SLOT = 0;
    public static final int DISK_SLOT = 1;
    public static final int ENERGY_SLOT = 2;
    public static final int INTERACTOR_SLOT = 3;

    public ShellBlockEntity(BlockPos pos, BlockState state) {
        super(RelayBlockEntities.SHELL_BLOCK_ENTITY, pos, state);
        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = ItemStack.EMPTY;
        }
        this.coreCount = 0;
        this.interval = 1;
        this.tickCounter = 0;
        this.initialized = false;
    }

    /**
     * Tick 方法
     */
    public static void tick(Level world, BlockPos pos, BlockState state, ShellBlockEntity entity) {
        if (world.isClientSide()) {
            return;
        }

        entity.updateCoreState();

        if (!entity.initialized) {
            entity.tryInitialize();
        }

        if (entity.initialized && entity.coreCount > 0) {
            entity.tickCounter++;
            if (entity.tickCounter >= entity.interval) {
                entity.tickCounter = 0;
            }
        }
    }

    /**
     * 更新核心状态（数量和 interval）
     */
    private void updateCoreState() {
        ItemStack coreStack = inventory[CORE_SLOT];
        if (!coreStack.isEmpty()) {
            coreCount = 1;
            interval = 1;
        } else {
            coreCount = 0;
            interval = 1;
        }
    }

    /**
     * 尝试初始化
     */
    private void tryInitialize() {
        ItemStack diskStack = inventory[DISK_SLOT];
        if (!diskStack.isEmpty()) {
            initialized = true;
        }
    }

    // ========== MenuProvider 接口 ==========

    @Override
    public Component getDisplayName() {
        return Component.literal("外壳");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        // TODO: 实现 GUI
        return null;
    }

    // ========== 状态访问 ==========

    public int getCoreCount() {
        return coreCount;
    }

    public int getInterval() {
        return interval;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
