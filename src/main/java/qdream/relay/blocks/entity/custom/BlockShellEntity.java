package qdream.relay.blocks.entity.custom;

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
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.NonNullList;
import java.util.List;
import java.util.UUID;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.core.ShellTickHandler;
import qdream.relay.core.ShellContainer;
import qdream.relay.core.ExecutionStats;
import qdream.relay.screen.ShellScreenHandler;
import qdream.relay.mc.StateMachineNbtSerializer;
import qdream.relay.mc.component.WorldInteractorComponent;
import qdream.relay.mc.component.DiskComponent;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.Relay;
import qdream.relay.core.ShellCoreGroupManager;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 外壳方块实体
 *
 * <h3>职责</h3>
 * <ul>
 * <li>实现 MenuProvider（GUI 支持）</li>
 * <li>实现 ShellContainer（外壳容器接口，继承自 Container）</li>
 * <li>Tick 逻辑（通过 ShellTickHandler）</li>
 * <li>NBT 持久化（ValueInput/ValueOutput）</li>
 * </ul>
 */
public class BlockShellEntity extends BlockEntity implements MenuProvider, ShellContainer {

    private static final int SLOT_COUNT = 4;
    private static final int LOG_BUFFER_SIZE = 200; // 日志缓冲区大小

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ShellTickHandler tickHandler;
    private final StateMachine stateMachine;
    private final ExecutionStats executionStats = new ExecutionStats();
    private Player owner;
    private UUID ownerUuid;
    private double energy;
    private boolean enabled;
    private UUID coreGroupId; // 所属核心共享组 ID

    /**
     * 日志缓冲区 - 存储最近的调试输出
     * 使用 ConcurrentLinkedQueue 保证线程安全
     */
    private final ConcurrentLinkedQueue<String> logBuffer = new ConcurrentLinkedQueue<>();

    public static final int CORE_SLOT = 0;
    public static final int DISK_SLOT = 1;
    public static final int ENERGY_SLOT = 2;
    public static final int INTERACTOR_SLOT = 3;

    public BlockShellEntity(BlockPos pos, BlockState state) {
        super(RelayBlockEntities.SHELL_BLOCK_ENTITY, pos, state);
        this.tickHandler = new ShellTickHandler();
        this.stateMachine = new StateMachine(Relay.DEFAULT_MAX_PROGRAM_STACK_SIZE);
        this.energy = 0;
        this.enabled = false;
        this.coreGroupId = null;

        // 设置事故回调
        stateMachine.setMishapHandler(reason -> {
            // 延迟获取 level，因为在构造函数中 level 为 null
            if (getLevel() != null && !getLevel().isClientSide()) {
                addLogEntry(String.format("§c§lMISHAP§r§c: %s", reason));
                // 同步日志到客户端
                syncLogsToClient(getLevel(), worldPosition);
            }
            setEnabled(false);
        });
    }

    /**
     * 格式化日志条目
     */
    private String formatLogEntry(String phase, Executable executable) {
        long tick = (getLevel() != null) ? getLevel().getGameTime() : 0;
        String id = "unknown";
        if (executable instanceof qdream.relay.mc.base.Operation op) {
            id = op.getClass().getSimpleName();
        }
        return String.format("[T%d] %s %s", tick, phase, id);
    }

    /**
     * 添加日志条目到缓冲区
     */
    private void addLogEntry(String log) {
        while (logBuffer.size() >= LOG_BUFFER_SIZE) {
            logBuffer.poll(); // 移除最旧的日志
        }
        logBuffer.offer(log);
    }

    /**
     * 获取日志缓冲区内容
     * 
     * @return 日志列表（按时间顺序）
     */
    public java.util.List<String> getLogBuffer() {
        return new java.util.ArrayList<>(logBuffer);
    }

    /**
     * 清空日志缓冲区
     */
    public void clearLogBuffer() {
        logBuffer.clear();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        // 方块被破坏时，从组中移除
        if (level != null && !level.isClientSide() && coreGroupId != null) {
            ShellCoreGroupManager.onBlockRemoved(level, worldPosition, this);
        }
    }

    /**
     * Tick 方法
     */
    public static void tick(Level world, BlockPos pos, BlockState state, BlockShellEntity entity) {
        // 在 tick 前设置上下文（level 和 self）
        var machine = entity.stateMachine;
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
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!inventory.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = inventory.get(slot);
        if (!stack.isEmpty()) {
            ItemStack result = stack.split(amount);
            setItem(slot, stack);
            setChanged();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = inventory.get(slot);
        if (!stack.isEmpty()) {
            inventory.set(slot, ItemStack.EMPTY);
            setChanged();
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.set(i, ItemStack.EMPTY);
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
        return stateMachine;
    }

    @Override
    public Player getOwner() {
        // 优先返回缓存的 owner（玩家仍在线）
        if (owner != null) {
            return owner;
        }
        // 玩家可能离线后重新上线，尝试从 UUID 恢复
        if (ownerUuid != null && getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // 使用 Server 级别的 PlayerList 查询，跨所有维度
            Player playerByUUID = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
            if (playerByUUID != null) {
                owner = playerByUUID;
                return owner;
            }
        }
        return null;
    }

    @Override
    public void setOwner(Player owner) {
        this.owner = owner;
        if (owner != null) {
            this.ownerUuid = owner.getUUID();
        }
        setChanged();
    }

    @Override
    public int getCoreCost() {
        if (coreGroupId == null || level == null || level.isClientSide()) {
            ItemStack coreStack = getCoreStack();
            if (!coreStack.isEmpty()) {
                if (coreStack.getItem() instanceof ComputingCoreComponent component) {
                    return component.getCost(coreStack);
                }
            }
            return 0;
        }

        // 使用 SavedData 获取组的所有成员，然后计算总 cost
        int totalCost = 0;
        List<BlockPos> members = ShellCoreGroupManager.getGroupMembers(level, coreGroupId);

        for (BlockPos memberPos : members) {
            BlockEntity be = level.getBlockEntity(memberPos);
            if (be instanceof BlockShellEntity shell) {
                ItemStack coreStack = shell.getCoreStack();
                if (!coreStack.isEmpty()) {
                    totalCost += coreStack.getCount();
                }
            }
        }

        return totalCost;
    }

    @Override
    public int getInterval() {
        if (coreGroupId == null || level == null || level.isClientSide()) {
            ItemStack coreStack = getCoreStack();
            if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
                return core.getInterval(coreStack);
            }
            return 0;
        }

        // 使用 SavedData 获取组的所有成员，然后计算最大 interval
        int maxInterval = 0;
        List<BlockPos> members = ShellCoreGroupManager.getGroupMembers(level, coreGroupId);

        for (BlockPos memberPos : members) {
            BlockEntity be = level.getBlockEntity(memberPos);
            if (be instanceof BlockShellEntity shell) {
                ItemStack coreStack = shell.getCoreStack();
                if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
                    int interval = core.getInterval(coreStack);
                    if (interval > maxInterval) {
                        maxInterval = interval;
                    }
                }
            }
        }

        return maxInterval;
    }

    @Override
    public double getEnergyCostPerTick() {
        if (coreGroupId == null || level == null || level.isClientSide()) {
            ItemStack coreStack = getCoreStack();
            if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
                return core.getEnergyCost(coreStack);
            }
            return 0.0;
        }

        // 使用 SavedData 获取组的所有成员，然后计算总能量消耗
        double totalEnergyCost = 0.0;
        List<BlockPos> members = ShellCoreGroupManager.getGroupMembers(level, coreGroupId);

        for (BlockPos memberPos : members) {
            BlockEntity be = level.getBlockEntity(memberPos);
            if (be instanceof BlockShellEntity shell) {
                ItemStack coreStack = shell.getCoreStack();
                if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
                    totalEnergyCost += core.getEnergyCost(coreStack);
                }
            }
        }

        return totalEnergyCost;
    }

    @Override
    public boolean isInitialized() {
        return tickHandler.isInitialized();
    }

    @Override
    public void setInitialized(boolean initialized) {
        tickHandler.setInitialized(initialized);
    }

    /**
     * 获取 GUI 开关状态
     * <p>
     * 此字段仅用于 GUI 显示，不直接影响执行逻辑
     * 执行逻辑由 {@link #canExecute()} 综合判断
     * </p>
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置 GUI 开关状态
     * <p>
     * 此方法仅改变 GUI 显示状态，不会清空程序栈
     * </p>
     */
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
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            return emi.getStoredEnergy(energyStack);
        }
        return 0;
    }

    @Override
    public void setEnergy(double energy) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            emi.setStoredEnergy(energyStack, energy);
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            syncEnergyToClient(level, worldPosition);
        }
    }

    @Override
    public boolean consumeEnergy(double amount) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            double consumed = emi.consumeEnergy(energyStack, amount);
            return consumed >= amount;
        }
        return false;
    }

    @Override
    public boolean isClientSide() {
        return level != null && level.isClientSide();
    }

    @Override
    public boolean hasOwner() {
        // 优先检查缓存的 owner
        if (owner instanceof Player) {
            return true;
        }
        // 如果 ownerUuid 存在，说明有保存的所有者（玩家可能暂时离线）
        return ownerUuid != null;
    }

    private DiskComponent getDiskComponent(ItemStack stack) {
        if (stack.getItem() instanceof DiskComponent) {
            return (DiskComponent) stack.getItem();
        }
        return null;
    }

    @Override
    public boolean hasWorldInteractor() {
        return getWorldInteractorStack().getItem() instanceof WorldInteractorComponent;
    }

    @Override
    public double addEnergy(double amount) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            double added = emi.addEnergy(energyStack, amount);
            // 同步到客户端
            setEnergy(emi.getStoredEnergy(energyStack));
            return added;
        }
        return 0;
    }

    @Override
    public void loadProgramFromDisk() {
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack diskStack = getDiskStack();
        if (diskStack.isEmpty()) {
            return;
        }

        DiskComponent diskComponent = getDiskComponent(diskStack);
        if (diskComponent == null) {
            return;
        }

        getStateMachine().clear();
        List<Executable> program = diskComponent.getProgram(diskStack);
        if (!program.isEmpty()) {
            getStateMachine().loadProgram(program);
            setInitialized(true);
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ========== ShellContainer 接口：执行统计 ==========

    @Override
    public ExecutionStats getExecutionStats() {
        return executionStats;
    }

    // ========== NBT 序列化与反序列化 ==========

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // 保存物品栏
        ContainerHelper.saveAllItems(output, inventory);

        // 保存能量
        output.putDouble("energy", energy);

        // 保存状态机状态
        CompoundTag machineTag = StateMachineNbtSerializer.INSTANCE.serialize(stateMachine);
        output.store("stateMachine", CompoundTag.CODEC, machineTag);

        // 保存开关状态
        output.putBoolean("enabled", enabled);

        // 保存所有者信息 - 使用 ownerUuid 判断，确保玩家离线后仍能保存
        if (ownerUuid != null) {
            output.putString("owner", ownerUuid.toString());
        }

        // 保存核心组 ID
        if (coreGroupId != null) {
            output.putString("coreGroupId", coreGroupId.toString());
        }

        // 保存 TickHandler 状态
        output.putInt("tickCounter", tickHandler.getTickCounter());
        output.putBoolean("initialized", tickHandler.isInitialized());

        // 保存执行统计
        CompoundTag statsTag = executionStats.toNbt();
        output.store("executionStats", CompoundTag.CODEC, statsTag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // 加载物品栏
        ContainerHelper.loadAllItems(input, inventory);

        // 加载能量
        energy = input.getIntOr("energy", 0);

        // 加载状态机状态
        input.read("stateMachine", CompoundTag.CODEC).ifPresent(machineTag -> {
            StateMachineNbtSerializer.INSTANCE.deserialize(stateMachine, (CompoundTag) machineTag);
        });

        // 加载开关状态
        enabled = input.getBooleanOr("enabled", false);

        // 加载所有者信息
        String uuidStr = input.getString("owner").orElse("");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUuid = java.util.UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        // 加载核心组 ID
        String groupIdStr = input.getString("coreGroupId").orElse("");
        if (!groupIdStr.isEmpty()) {
            try {
                coreGroupId = java.util.UUID.fromString(groupIdStr);
            } catch (IllegalArgumentException e) {
                // UUID 格式错误，忽略
            }
        }

        // 加载 TickHandler 状态
        tickHandler.setTickCounter(input.getIntOr("tickCounter", 0));
        tickHandler.setInitialized(input.getBooleanOr("initialized", false));

        // 加载执行统计
        input.read("executionStats", CompoundTag.CODEC).ifPresent(statsTag -> {
            executionStats.fromNbt((CompoundTag) statsTag);
        });
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

    /**
     * 同步日志到客户端
     */
    public void syncLogsToClient(Level world, BlockPos pos) {
        if (world.isClientSide()) {
            return;
        }

        java.util.List<String> logs = getLogBuffer();
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) world;
        net.minecraft.world.level.ChunkPos chunkPos = net.minecraft.world.level.ChunkPos.containing(pos);
        serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)
                .forEach(player -> {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player,
                            new qdream.relay.networking.payloads.S2C_ShellLogPayload(logs));
                });
    }

    @Override
    public ItemStack getCoreStack() {
        return inventory.get(CORE_SLOT);
    }

    @Override
    public ItemStack getDiskStack() {
        return inventory.get(DISK_SLOT);
    }

    @Override
    public ItemStack getEnergyStack() {
        return inventory.get(ENERGY_SLOT);
    }

    @Override
    public ItemStack getWorldInteractorStack() {
        return inventory.get(INTERACTOR_SLOT);
    }

    /**
     * 获取核心组 ID
     */
    public UUID getCoreGroupId() {
        return coreGroupId;
    }

    /**
     * 设置核心组 ID
     */
    public void setCoreGroupId(UUID groupId) {
        this.coreGroupId = groupId;
        setChanged();
    }
}
