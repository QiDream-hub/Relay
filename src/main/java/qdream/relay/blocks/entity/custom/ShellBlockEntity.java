package qdream.relay.blocks.entity.custom;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import java.util.List;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.core.ShellStateManager;
import qdream.relay.core.ShellTickHandler;
import qdream.relay.core.ShellContainer;
import qdream.relay.screen.ShellScreenHandler;
import qdream.relay.core.ShellRegistry;
import qdream.relay.items.SpellDiskItem;
import qdream.relay.mc.StateMachineNbtSerializer;
import qdream.relay.mc.component.WorldInteractorComponent;

/**
 * 外壳方块实体
 * 
 * <p>
 * 使用 {@link ShellStateManager} 管理物品栏、StateMachine、Owner 状态
 * </p>
 * 
 * <h3>职责</h3>
 * <ul>
 * <li>实现 Container 接口（物品栏插槽访问）</li>
 * <li>实现 MenuProvider（GUI 支持）</li>
 * <li>实现 ShellContainer（外壳容器接口）</li>
 * <li>Tick 逻辑（通过 ShellTickHandler）</li>
 * <li>NBT 持久化（ValueInput/ValueOutput）</li>
 * </ul>
 */
public class ShellBlockEntity extends BlockEntity implements MenuProvider, Container, ShellContainer {

    private final ShellStateManager stateManager;
    private final ShellTickHandler tickHandler;
    private double energy;
    private boolean enabled;

    public static final int CORE_SLOT = 0;
    public static final int DISK_SLOT = 1;
    public static final int ENERGY_SLOT = 2;
    public static final int INTERACTOR_SLOT = 3;

    public ShellBlockEntity(BlockPos pos, BlockState state) {
        super(RelayBlockEntities.SHELL_BLOCK_ENTITY, pos, state);
        this.stateManager = new ShellStateManager();
        this.tickHandler = new ShellTickHandler();
        this.energy = 0;
        this.enabled = false;

        // 设置事故回调
        stateManager.getStateMachine().setMishapHandler(reason -> {
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        });

        // 注册到 ShellRegistry
        if (level != null && !level.isClientSide()) {
            ShellRegistry.register(this, pos);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        ShellRegistry.unregister(this);
    }

    /**
     * Tick 方法
     */
    public static void tick(Level world, BlockPos pos, BlockState state, ShellBlockEntity entity) {
        // 在 tick 前设置上下文（level 和 self）
        var machine = entity.stateManager.getStateMachine();
        if (machine.isRunning()) {
            machine.setContext("level", world);
            machine.setContext("self", entity);
        }

        entity.tickHandler.tick(entity);

        // 每 20 tick 同步一次能量到客户端（兜底同步）
        if (!world.isClientSide() && world.getGameTime() % 20 == 0) {
            entity.syncEnergyToClient(world, pos);
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

    // ========== Container 接口 ==========

    @Override
    public int getContainerSize() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < 4; i++) {
            if (!stateManager.getInventorySlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return stateManager.getInventorySlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = stateManager.getInventorySlot(slot);
        if (!stack.isEmpty()) {
            ItemStack result = stack.split(amount);
            setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = stateManager.getInventorySlot(slot);
        if (!stack.isEmpty()) {
            stateManager.setInventorySlot(slot, ItemStack.EMPTY);
            setChanged();
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stateManager.setInventorySlot(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < 4; i++) {
            stateManager.setInventorySlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level != null && this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return true;
    }

    // ========== ShellContainer 接口 ==========

    @Override
    public StateMachine getStateMachine() {
        return stateManager.getStateMachine();
    }

    @Override
    public ItemStack getInventorySlot(int slot) {
        return stateManager.getInventorySlot(slot);
    }

    @Override
    public void setInventorySlot(int slot, ItemStack stack) {
        stateManager.setInventorySlot(slot, stack);
        setChanged();
    }

    @Override
    public Entity getOwner() {
        // 优先返回直接持有的 owner 字段
        if (stateManager.getOwner() != null) {
            return stateManager.getOwner();
        }
        // 延迟加载作为兜底
        if (stateManager.getOwnerUuid() != null && level != null && !level.isClientSide()) {
            Entity owner = level.getEntity(stateManager.getOwnerUuid());
            if (owner != null) {
                stateManager.setOwner(owner);
            }
        }
        return stateManager.getOwner();
    }

    @Override
    public void setOwner(Entity owner) {
        stateManager.setOwner(owner);
        setChanged();
    }

    @Override
    public int getCoreCost() {
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
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public double getEnergy() {
        return energy;
    }

    @Override
    public void setEnergy(double energy) {
        this.energy = energy;
        setChanged();
        // 事件驱动：能量变化时立即同步到客户端
        if (level != null && !level.isClientSide()) {
            syncEnergyToClient(level, worldPosition);
        }
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    /**
     * 复位程序 - 清空双栈后从磁盘重新加载程序
     */
    public void resetProgram() {
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack diskStack = getDiskStack();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof SpellDiskItem)) {
            return;
        }

        stateManager.getStateMachine().clear();
        List<Executable> program = SpellDiskItem.getProgram(diskStack);
        if (!program.isEmpty()) {
            stateManager.getStateMachine().loadProgram(program);
            setInitialized(true);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public ItemStack getCoreStack() {
        return stateManager.getInventorySlot(CORE_SLOT);
    }

    @Override
    public ItemStack getDiskStack() {
        return stateManager.getInventorySlot(DISK_SLOT);
    }

    @Override
    public ItemStack getEnergyStack() {
        return stateManager.getInventorySlot(ENERGY_SLOT);
    }

    @Override
    public ItemStack getInteractorStack() {
        return stateManager.getInventorySlot(INTERACTOR_SLOT);
    }

    @Override
    public boolean hasOwner() {
        if (this.stateManager.getOwner() != null && this.stateManager.getOwner() instanceof Player) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasWorldInteractor() {
        if (getInteractorStack().getItem() instanceof WorldInteractorComponent) {
            return true;
        }
        return false;
    }
    // ========== NBT 序列化与反序列化 ==========

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // 保存物品栏
        ContainerHelper.saveAllItems(output, stateManager.getInventory());

        // 保存能量
        output.putDouble("energy", energy);

        // 保存状态机状态
        CompoundTag machineTag = StateMachineNbtSerializer.INSTANCE.serialize(stateManager.getStateMachine());
        output.store("stateMachine", CompoundTag.CODEC, machineTag);

        // 保存开关状态
        output.putBoolean("enabled", enabled);

        // 保存所有者信息
        if (stateManager.getOwner() != null) {
            output.putString("owner", stateManager.getOwner().getUUID().toString());
        }

        // 保存 TickHandler 状态
        output.putInt("tickCounter", tickHandler.getTickCounter());
        output.putInt("coreCount", tickHandler.getCoreCount());
        output.putInt("interval", tickHandler.getInterval());
        output.putBoolean("initialized", tickHandler.isInitialized());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // 加载物品栏
        ContainerHelper.loadAllItems(input, stateManager.getInventory());

        // 加载能量
        energy = input.getIntOr("energy", 0);

        // 加载状态机状态
        input.read("stateMachine", CompoundTag.CODEC).ifPresent(machineTag -> {
            StateMachineNbtSerializer.INSTANCE.deserialize(stateManager.getStateMachine(), (CompoundTag) machineTag);
        });

        // 加载开关状态
        enabled = input.getBooleanOr("enabled", false);

        // 加载所有者信息
        String uuidStr = input.getString("owner").orElse("");
        if (!uuidStr.isEmpty()) {
            try {
                java.util.UUID ownerUuid = java.util.UUID.fromString(uuidStr);
                stateManager.setOwnerUuid(ownerUuid);
            } catch (IllegalArgumentException e) {
                // UUID 格式错误，忽略
            }
        }

        // 加载 TickHandler 状态
        tickHandler.setTickCounter(input.getIntOr("tickCounter", 0));
        tickHandler.setCoreCount(input.getIntOr("coreCount", 0));
        tickHandler.setInterval(input.getIntOr("interval", 0));
        tickHandler.setInitialized(input.getBooleanOr("initialized", false));
    }

    // ========== 网络同步 ==========

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }

    /**
     * 同步能量值到客户端
     */
    public void syncEnergyToClient(Level world, BlockPos pos) {
        if (world.isClientSide()) {
            return;
        }

        double energy = getEnergy();
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) world;
        net.minecraft.world.level.ChunkPos chunkPos = net.minecraft.world.level.ChunkPos.containing(pos);
        serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)
                .forEach(player -> {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player,
                            new qdream.relay.networking.payloads.S2C_ShellEnergyPayload(energy));
                });
    }
}
