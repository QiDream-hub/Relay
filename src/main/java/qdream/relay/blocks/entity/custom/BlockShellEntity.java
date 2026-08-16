package qdream.relay.blocks.entity.custom;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.NonNullList;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.engine.Warning;
import qdream.relay.blocks.entity.RelayBlockEntities;
import qdream.relay.core.ShellTickHandler;
import qdream.relay.core.ShellContainer;
import qdream.relay.core.ExecutionStats;
import qdream.relay.screen.BlockShellScreenHandler;
import qdream.relay.mc.StateMachineNbtSerializer;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.component.WorldInteractorComponent;
import qdream.relay.mc.component.DiskComponent;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.Relay;
import qdream.relay.core.ShellCoreGroupManager;
import qdream.relay.tools.TextTools;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import qdream.relay.networking.payloads.S2C_ShellLogPushPayload;
import qdream.relay.networking.payloads.S2C_ClearLogsPayload;

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

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ShellTickHandler tickHandler;
    private final StateMachine stateMachine;
    private ExecutionStats executionStats; // 在 enabled=true 时创建
    private Player owner;
    private UUID ownerUuid;
    private double energy;
    private boolean enabled; // true = 已初始化并允许运行
    private UUID coreGroupId; // 所属核心共享组 ID
    private boolean debugOutputEnabled; // 是否启用调试输出
    private boolean statusInfoEnabled; // 是否启用统计信息

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
        this.executionStats = null; // 初始为 null，在 enabled=true 时创建

        // 设置事故回调
        stateMachine.setMishapHandler(warning -> {
            // 延迟获取 level，因为在构造函数中 level 为 null
            if (getLevel() != null && !getLevel().isClientSide()) {
                MutableComponent log = Component.literal("§lMISHAP:").withColor(0xFF5555);
                if (warning.getInfo() instanceof Component component) {
                    log.append(component);
                } else {
                    log.append(Component.literal(warning.getMessage()));
                }
                // 实时推送单条日志到客户端
                pushLogToClient(getLevel(), worldPosition, log);
            }
            setEnabled(false); // 事故时关闭
        });
        // 设置调试回调
        tickHandler.setDebugCallback(new ShellTickHandler.DebugCallback() {
            @Override
            public void afterStep(StateMachine stateMachine, Executable executable) {
                if (isDebugOutputEnabled()) {
                    if (getLevel() != null && !getLevel().isClientSide()) {
                        Component separatorLog = Component.translatable("gui.relay:shell.debug.separator");
                        Component programStackLog = Component.translatable(
                                "gui.relay:shell.debug.program_stack",
                                TextTools.formatProgramStack(stateMachine));
                        Component dataStackLog = Component.translatable(
                                "gui.relay:shell.debug.data_stack",
                                TextTools.formatDataStack(stateMachine));

                        // 实时推送单条日志到客户端
                        pushLogToClient(getLevel(), worldPosition, separatorLog);
                        pushLogToClient(getLevel(), worldPosition, programStackLog);
                        pushLogToClient(getLevel(), worldPosition, dataStackLog);
                    }
                }
            }

            @Override
            public void onMishap(StateMachine stateMachine, Executable executable, Warning warning) {
                if (isDebugOutputEnabled()) {
                    if (getLevel() != null && !getLevel().isClientSide()) {
                        String opName = "unknown";
                        if (executable instanceof Operation op) {
                            opName = op.getId();
                        }
                        Component separatorLog = Component.translatable("gui.relay:shell.debug.separator");
                        Component mishapTitleLog = Component.translatable(
                                "gui.relay:shell.mishap.title",
                                Component.literal(opName));
                        Component mishapReasonLog = Component.translatable(
                                "gui.relay:shell.mishap.reason",
                                Component.literal(warning.getMessage()));
                        Component programStackLog = Component.translatable(
                                "gui.relay:shell.debug.program_stack",
                                TextTools.formatProgramStack(stateMachine));
                        Component dataStackLog = Component.translatable(
                                "gui.relay:shell.debug.data_stack",
                                TextTools.formatDataStack(stateMachine));

                        // 实时推送单条日志到客户端
                        pushLogToClient(getLevel(), worldPosition, separatorLog);
                        pushLogToClient(getLevel(), worldPosition, mishapTitleLog);
                        pushLogToClient(getLevel(), worldPosition, mishapReasonLog);
                        pushLogToClient(getLevel(), worldPosition, programStackLog);
                        pushLogToClient(getLevel(), worldPosition, dataStackLog);
                    }
                }
            }
        });
    }

    /**
     * 实时推送单条日志到客户端
     * 新客户端使用此方法接收单条日志并缓存到本地
     *
     * @param world 世界
     * @param pos   方块坐标
     * @param log   日志内容
     */
    private void pushLogToClient(Level world, BlockPos pos, Component log) {
        if (world.isClientSide()) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) world;
        net.minecraft.world.level.ChunkPos chunkPos = net.minecraft.world.level.ChunkPos.containing(pos);
        serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)
                .forEach(player -> {
                    ServerPlayNetworking.send(player, new S2C_ShellLogPushPayload(pos, log));
                });
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        // 方块被破坏时，从组中移除
        // 即使 coreGroupId 为 null 也要调用，因为 SavedData 中可能仍有记录
        if (level != null && !level.isClientSide()) {
            ShellCoreGroupManager.onBlockRemoved(level, worldPosition, this);

            // 发送清理日志缓存网络包到客户端
            ServerLevel serverLevel = (ServerLevel) level;
            net.minecraft.world.level.ChunkPos chunkPos = net.minecraft.world.level.ChunkPos.containing(worldPosition);
            serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)
                    .forEach(player -> {
                        ServerPlayNetworking.send(player, new S2C_ClearLogsPayload(worldPosition));
                    });
        }
    }

    /**
     * Tick 方法
     */
    public static void tick(Level world, BlockPos pos, BlockState state, BlockShellEntity entity) {
        var machine = entity.stateMachine;

        // 自动加载程序：如果 enabled=true 但程序未运行，从磁盘加载
        if (!world.isClientSide() && entity.isEnabled() && !entity.isRunning()) {
            entity.loadProgramFromDisk();
        }

        // 在加载程序后设置上下文（level 和 self），保证 GetSelf 等操作能正确获取自身引用
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
        return Component.literal("");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new BlockShellScreenHandler(syncId, inv, this);
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
        if (ownerUuid != null && getLevel() instanceof ServerLevel serverLevel) {
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
        if (level == null || level.isClientSide()) {
            ItemStack coreStack = getCoreStack();
            if (!coreStack.isEmpty()) {
                if (coreStack.getItem() instanceof ComputingCoreComponent component) {
                    return component.getCost(coreStack);
                }
            }
            return 0;
        }

        // 从 SavedData 动态查询当前坐标的组 ID
        UUID groupId = ShellCoreGroupManager.getGroupIdForPosition(level, worldPosition);
        if (groupId == null) {
            // 不属于任何组，返回单个核心的 cost
            ItemStack coreStack = getCoreStack();
            if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent component) {
                return component.getCost(coreStack);
            }
            return 0;
        }

        // 使用 SavedData 获取组的所有成员，然后计算总 cost
        int totalCost = 0;
        List<BlockPos> members = ShellCoreGroupManager.getGroupMembers(level, groupId);

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
        if (level == null || level.isClientSide()) {
            ItemStack coreStack = getCoreStack();
            if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
                return core.getInterval(coreStack);
            }
            return 0;
        }

        // 从 SavedData 动态查询当前坐标的组 ID
        UUID groupId = ShellCoreGroupManager.getGroupIdForPosition(level, worldPosition);
        if (groupId == null) {
            // 不属于任何组，返回单个核心的 interval
            ItemStack coreStack = getCoreStack();
            if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
                return core.getInterval(coreStack);
            }
            return 0;
        }

        // 使用 SavedData 获取组的所有成员，然后计算最大 interval
        int maxInterval = 0;
        List<BlockPos> members = ShellCoreGroupManager.getGroupMembers(level, groupId);

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
        if (level == null || level.isClientSide()) {
            ItemStack coreStack = getCoreStack();
            if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
                return core.getEnergyCost(coreStack);
            }
            return 0.0;
        }

        // 从 SavedData 动态查询当前坐标的组 ID
        UUID groupId = ShellCoreGroupManager.getGroupIdForPosition(level, worldPosition);
        if (groupId == null) {
            // 不属于任何组，返回单个核心的能量消耗
            ItemStack coreStack = getCoreStack();
            if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
                return core.getEnergyCost(coreStack);
            }
            return 0.0;
        }

        // 使用 SavedData 获取组的所有成员，然后计算总能量消耗
        double totalEnergyCost = 0.0;
        List<BlockPos> members = ShellCoreGroupManager.getGroupMembers(level, groupId);

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

    /**
     * 获取 GUI 开关状态
     * <p>
     * 此字段用于 GUI 显示和执行控制
     * 当 enabled=false 时，{@link #canExecute()} 返回 false，tick 逻辑跳过
     * 当 enabled=true 时，如果程序未运行会自动加载程序
     * </p>
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置 GUI 开关状态
     * <p>
     * 切换开关时，会立即影响 {@link #canExecute()} 的判断结果
     * 程序加载在 tick 中自动处理（当 enabled=true 且 isRunning=false 时）
     * </p>
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
    }

    @Override
    public boolean canExecute() {
        // BlockShell 需要检查 enabled 状态和运行状态
        return isEnabled() && isRunning();
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
    public boolean isWorldInRange(Vec3 sourcePos, Vec3 targetPos) {
        ItemStack interactorStack = getWorldInteractorStack();
        if (!interactorStack.isEmpty() && interactorStack.getItem() instanceof WorldInteractorComponent wic) {
            return wic.isInRange(interactorStack, sourcePos, targetPos);
        }
        return false;
    }

    @Override
    public double getWorldInteractorEnergyCost() {
        ItemStack interactorStack = getWorldInteractorStack();
        if (!interactorStack.isEmpty() && interactorStack.getItem() instanceof WorldInteractorComponent wic) {
            return wic.getEnergyCost(interactorStack);
        }
        return 0.0;
    }

    @Override
    public double getWorldInteractorRange() {
        ItemStack interactorStack = getWorldInteractorStack();
        if (!interactorStack.isEmpty() && interactorStack.getItem() instanceof WorldInteractorComponent wic) {
            return wic.getRange(interactorStack);
        }
        return 0.0;
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

    public void loadProgramFromDisk() {
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack diskStack = getDiskStack();
        if (diskStack.isEmpty()) {
            pushLogToClient(getLevel(), worldPosition, Component.translatable("gui.relay:shell.program_reload.disk_empty"));
            return;
        }

        DiskComponent diskComponent = getDiskComponent(diskStack);
        if (diskComponent == null) {
            pushLogToClient(getLevel(), worldPosition, Component.translatable("gui.relay:shell.program_reload.disk_component_null"));
            return;
        }

        getStateMachine().clear();
        String programJson = diskComponent.getProgram(diskStack);
        List<Executable> program;
        try {
            program = ProgramCompiler.compileFromJson(programJson);
        } catch (ProgramCompiler.CompilationException e) {
            e.printStackTrace();
            program = new ArrayList<>();
        }

        if (program.isEmpty()) {
            pushLogToClient(getLevel(), worldPosition, Component.translatable("gui.relay:shell.program_reload.program_empty"));
            return;
        }

        getStateMachine().loadProgram(program);
        setChanged();
        pushLogToClient(getLevel(), worldPosition, Component.translatable("gui.relay:shell.program_reload.success", program.size()));
    }

    // ========== ShellContainer 接口：执行统计 ==========

    @Override
    public ExecutionStats getExecutionStats() {
        // 如果 executionStats 为 null（enabled=false 时），返回一个空的统计对象
        if (executionStats == null) {
            return new ExecutionStats();
        }
        return executionStats;
    }

    // ========== NBT 序列化与反序列化 ==========

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        // 保存物品栏
        ContainerHelper.saveAllItems(output, inventory);

        // 保存配置到统一的 CompoundTag
        CompoundTag configTag = new CompoundTag();
        configTag.putDouble("Energy", energy);
        configTag.putBoolean("Enabled", enabled);
        output.store("Config", CompoundTag.CODEC, configTag);

        // 保存状态机状态
        CompoundTag machineTag = StateMachineNbtSerializer.INSTANCE.serialize(stateMachine);
        output.store("StateMachine", CompoundTag.CODEC, machineTag);

        // 保存所有者信息 - 使用 ownerUuid 判断，确保玩家离线后仍能保存
        if (ownerUuid != null) {
            output.putString("OwnerUUID", ownerUuid.toString());
        }

        // 保存核心组 ID
        if (coreGroupId != null) {
            output.putString("CoreGroupId", coreGroupId.toString());
        }

        // 保存调试配置
        CompoundTag debugConfigTag = new CompoundTag();
        debugConfigTag.putBoolean("DebugOutputEnabled", debugOutputEnabled);
        debugConfigTag.putBoolean("StatusInfoEnabled", statusInfoEnabled);
        output.store("DebugConfig", CompoundTag.CODEC, debugConfigTag);

        // 保存 TickHandler 状态（使用 ShellTickHandler 自己的序列化方法）
        CompoundTag tickHandlerTag = tickHandler.toNbt();
        output.store("TickHandler", CompoundTag.CODEC, tickHandlerTag);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // 加载物品栏
        ContainerHelper.loadAllItems(input, inventory);

        // 加载配置从统一的 CompoundTag
        input.read("Config", CompoundTag.CODEC).ifPresent(configTag -> {
            energy = configTag.getDouble("Energy").orElse(0.0);
            enabled = configTag.getBoolean("Enabled").orElse(false);
        });

        // 加载状态机状态
        input.read("StateMachine", CompoundTag.CODEC).ifPresent(machineTag -> {
            StateMachineNbtSerializer.INSTANCE.deserialize(stateMachine, (CompoundTag) machineTag);
        });

        // 加载所有者信息
        String uuidStr = input.getString("OwnerUUID").orElse("");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUuid = java.util.UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }

        // 加载核心组 ID
        String groupIdStr = input.getString("CoreGroupId").orElse("");
        if (!groupIdStr.isEmpty()) {
            try {
                coreGroupId = java.util.UUID.fromString(groupIdStr);
            } catch (IllegalArgumentException e) {
                // UUID 格式错误，忽略
            }
        }

        // 加载调试配置
        input.read("DebugConfig", CompoundTag.CODEC).ifPresent(debugConfigTag -> {
            debugOutputEnabled = debugConfigTag.getBoolean("DebugOutputEnabled").orElse(false);
            statusInfoEnabled = debugConfigTag.getBoolean("StatusInfoEnabled").orElse(false);
        });

        // 加载 TickHandler 状态（使用 ShellTickHandler 自己的反序列化方法）
        input.read("TickHandler", CompoundTag.CODEC).ifPresent(tag -> {
            tickHandler.fromNbt(tag);
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
        ServerLevel serverLevel = (ServerLevel) world;
        net.minecraft.world.level.ChunkPos chunkPos = net.minecraft.world.level.ChunkPos.containing(pos);
        serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false)
                .forEach(player -> {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                            player,
                            new qdream.relay.networking.payloads.S2C_ShellEnergyPayload(energy));
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

    // ========== 调试配置 ==========

    /**
     * 是否启用调试输出
     *
     * @return true 如果启用调试输出
     */
    public boolean isDebugOutputEnabled() {
        return debugOutputEnabled;
    }

    /**
     * 设置是否启用调试输出
     *
     * @param enabled true 启用调试输出
     */
    public void setDebugOutputEnabled(boolean enabled) {
        this.debugOutputEnabled = enabled;
        setChanged();
    }

    /**
     * 是否启用统计信息
     *
     * @return true 如果启用统计信息
     */
    public boolean isStatusInfoEnabled() {
        return statusInfoEnabled;
    }

    /**
     * 设置是否启用统计信息
     *
     * @param enabled true 启用统计信息
     */
    public void setStatusInfoEnabled(boolean enabled) {
        this.statusInfoEnabled = enabled;
        if (enabled) {
            executionStats = new ExecutionStats();
        }
        if (!enabled && !this.getLevel().isClientSide()
                && this.executionStats != null) {
            // 关闭时打印统计信息（仅当启用统计信息且尚未输出时）
            Component separatorLog = Component.translatable("gui.relay:shell.debug.separator");
            Component statsTitleLog = Component.translatable("gui.relay:shell.stats.title");
            
            pushLogToClient(getLevel(), worldPosition, separatorLog);
            pushLogToClient(getLevel(), worldPosition, statsTitleLog);
            
            String[] formatStatsPanel = this.executionStats.formatStatsPanel();
            for (String string : formatStatsPanel) {
                pushLogToClient(getLevel(), worldPosition, Component.literal(string));
            }
            pushLogToClient(getLevel(), worldPosition, separatorLog);
            
            this.executionStats = null;
        }
        setChanged();
    }
}
