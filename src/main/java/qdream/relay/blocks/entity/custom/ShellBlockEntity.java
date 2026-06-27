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
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import java.util.List;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.core.ShellContainer;
import qdream.relay.core.ShellTickHandler;
import qdream.relay.screen.ShellScreenHandler;
import qdream.relay.mc.StateMachineNbtSerializer;
import qdream.relay.core.ShellRegistry;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.items.SpellDiskItem;

/**
 * 外壳方块实体
 * 维护状态机，执行 tick，处理持久化
 */
public class ShellBlockEntity extends BlockEntity implements MenuProvider, ShellContainer, Container {

    // 使用 NonNullList 替代数组，支持 ContainerHelper 序列化
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(4, ItemStack.EMPTY);
    private final StateMachine stateMachine;
    private final ShellTickHandler tickHandler;

    private double energy;
    private boolean enabled;
    private Entity owner;
    private java.util.UUID ownerUuid;

    public ShellBlockEntity(BlockPos pos, BlockState state) {
        super(RelayBlockEntities.SHELL_BLOCK_ENTITY, pos, state);
        this.stateMachine = new StateMachine(1024);
        this.tickHandler = new ShellTickHandler();
        this.energy = 0;
        this.enabled = false;
        this.owner = null;
        this.ownerUuid = null;

        // 设置事故回调
        this.stateMachine.setMishapHandler(reason -> {
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
        // 从 ShellRegistry 注销
        ShellRegistry.unregister(this);
    }

    /**
     * Tick 方法
     */
    public static void tick(Level world, BlockPos pos, BlockState state, ShellBlockEntity entity) {
        entity.tickHandler.tick(entity);
        
        // 每 10 tick 同步一次能量到客户端
        if (!world.isClientSide() && world.getGameTime() % 10 == 0) {
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

    // ========== ShellContainer 接口 ==========

    @Override
    public ItemStack getInventorySlot(int slot) {
        if (slot >= 0 && slot < inventory.size()) {
            return inventory.get(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < inventory.size()) {
            inventory.set(slot, stack);
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
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
        // 通知客户端同步
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
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    /**
     * 复位程序 - 清空双栈后从磁盘重新加载程序
     * 点击复位按钮时调用
     */
    public void resetProgram() {
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack diskStack = getDiskStack();
        if (diskStack.isEmpty() || !(diskStack.getItem() instanceof SpellDiskItem)) {
            return;
        }

        // 清空状态机双栈
        stateMachine.clear();

        // 从磁盘读取程序
        List<Executable> program = SpellDiskItem.getProgram(diskStack);
        if (program.isEmpty()) {
            return;
        }

        // 加载到状态机
        stateMachine.loadProgram(program);
        setInitialized(true);
        setChanged();

        // 通知客户端同步
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        // 根据插槽类型限制可放置的物品
        return true; // 允许所有物品，具体限制在 GUI 中处理
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

    // ========== ShellContainer 接口 - 所有者管理 ==========

    @Override
    public Entity getOwner() {
        // 如果 owner 为空但有保存的 UUID，尝试延迟加载
        if (owner == null && ownerUuid != null && level != null && !level.isClientSide()) {
            owner = level.getEntity(ownerUuid);
        }
        return owner;
    }

    @Override
    public void setOwner(Entity owner) {
        this.owner = owner;
        if (owner != null) {
            this.ownerUuid = owner.getUUID();
        }
        setChanged();
    }

    // ========== NBT 序列化与反序列化 (26.1.2 ValueInput/ValueOutput) ==========

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // 保存物品栏 - 使用 ContainerHelper 处理 DataComponent 系统
        ContainerHelper.saveAllItems(output, this.inventory);

        // 保存能量
        output.putDouble("energy", energy);

        // 保存状态机状态 - 使用 CompoundTag.CODEC 序列化
        CompoundTag machineTag = StateMachineNbtSerializer.INSTANCE.serialize(stateMachine);
        output.store("stateMachine", CompoundTag.CODEC, machineTag);

        // 保存开关状态
        output.putBoolean("enabled", enabled);

        // 保存所有者信息
        if (owner != null) {
            output.putString("owner", owner.getUUID().toString());
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

        // 加载物品栏 - 使用 ContainerHelper 处理 DataComponent 系统
        ContainerHelper.loadAllItems(input, this.inventory);

        // 加载能量
        energy = input.getIntOr("energy", 0);

        // 加载状态机状态
        input.read("stateMachine", CompoundTag.CODEC).ifPresent(machineTag -> {
            StateMachineNbtSerializer.INSTANCE.deserialize(stateMachine, (CompoundTag) machineTag);
        });

        // 加载开关状态
        enabled = input.getBooleanOr("enabled", false);

        // 加载所有者信息 - 只保存 UUID，延迟加载实体
        String uuidStr = input.getString("owner").orElse("");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUuid = java.util.UUID.fromString(uuidStr);
                // 不立即获取实体，等 getOwner() 时再延迟加载
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

    // ========== 同步数据包 ==========

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }

    // ========== 状态访问 ==========
    public void updateStatus() {
        tickHandler.updateCoreState(this);
        tickHandler.updateEnergy(this);
    }

    // ========== 网络同步 ==========
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
                    new qdream.relay.networking.payloads.S2C_ShellEnergyPayload(energy)
                );
            });
    }
}
