package qdream.relay.items.container;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.NonNullList;

import qdream.relay.Component.RelayDataComponents;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.ToolShellItem;
import qdream.relay.core.ShellContainer;
import qdream.relay.core.ShellTickHandler;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.mc.component.WorldInteractorComponent;
import qdream.relay.mc.component.DiskComponent;
import qdream.relay.tools.StackTools;
import qdream.relay.mc.StateMachineNbtSerializer;
import qdream.relay.Relay;

import java.util.List;
import java.util.UUID;

/**
 * 工具外壳的 ShellContainer 实现
 *
 * <h3>存储结构</h3>
 *
 * <pre>
 * TOOL_SHELL_DATA: {
 *   "inventory": ListTag,           // 4 个插槽
 *   "stateMachine": {...},          // StateMachine NBT
 *   "owner": "uuid-string"          // 拥有者 UUID
 * }
 * TOOL_SHELL_TICK_STATE: {
 *   "tickCounter": int,
 *   "initialized": boolean
 * }
 * TOOL_SHELL_CONFIG: {
 *   "useInventoryEnergyModule": boolean
 * }
 * TOOL_SHELL_SESSION_ID: {
 *   "session-id-string"             // 会话 ID（UUID 字符串）
 * }
 * </pre>
 */
public class ToolShellContainer implements ShellContainer {

    public static final int CORE_SLOT = 0;
    public static final int DISK_SLOT = 1;
    public static final int ENERGY_SLOT = 2;
    public static final int INTERACTOR_SLOT = 3;

    private static final int SLOT_COUNT = 4;

    final ToolShellItem toolShell; // package-private for direct access
    ItemStack stack; // package-private - 非 final 以支持引用更新
    private final UUID sessionId; // 会话 ID
    private final NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final StateMachine stateMachine;
    private final ShellTickHandler tickHandler = new ShellTickHandler();
    private Entity owner;
    private UUID ownerUuid;

    public ToolShellContainer(ToolShellItem toolShell, ItemStack stack, UUID sessionId) {
        this.toolShell = toolShell;
        this.stack = stack;
        this.sessionId = sessionId;
        this.stateMachine = new StateMachine(Relay.DEFAULT_MAX_PROGRAM_STACK_SIZE);

        loadAllState();

        // 设置事故回调
        stateMachine.setMishapHandler(reason -> {
            Entity owner = ToolShellContainer.this.owner;
            if (owner != null && owner instanceof Player player) {
                player.sendSystemMessage(Component.literal("§c[工具外壳] 事故：" + reason));
            }
        });
        // 设置调试回调
        tickHandler.setDebugCallback((stateMachine, phase, executable) -> {
            if (isDebugOutputEnabled()) {
                Entity owner = ToolShellContainer.this.owner;
                if (owner != null && owner instanceof Player player) {
                    // mishap: 显示操作和双栈
                    if ("mishap".equals(phase)) {
                        String opName = "unknown";
                        if (executable instanceof Operation op) {
                            opName = op.getId();
                        }
                        player.sendSystemMessage(Component.literal(
                                "§c[§c 事故 §c] §f操作：" + opName));
                        player.sendSystemMessage(Component.literal(
                                "§7[§f 程序栈 §7]: " + StackTools.formatProgramStack(stateMachine)));
                        player.sendSystemMessage(Component.literal(
                                "§7[§f 数据栈 §7]: " + StackTools.formatDataStack(stateMachine)));
                    }
                    // afterStep: 只显示双栈
                    else if ("afterStep".equals(phase)) {
                        player.sendSystemMessage(Component.literal(
                                "§7[§f 程序栈 §7]: " + StackTools.formatProgramStack(stateMachine)));
                        player.sendSystemMessage(Component.literal(
                                "§7[§f 数据栈 §7]: " + StackTools.formatDataStack(stateMachine)));
                    }
                    player.sendSystemMessage(Component.literal("§8§m----------------------------------------"));
                }
            }
        });
    }

    /**
     * 获取 ItemStack
     */
    public ItemStack getStack() {
        return stack;
    }

    /**
     * 更新持有的 ItemStack 引用
     * <p>
     * 当物品在玩家物品栏中移动时，Minecraft 会创建新的 ItemStack 实例，
     * 但 DataComponent 会被复制。此方法确保 Container 持有最新的 ItemStack 引用，
     * 避免状态保存到错误的 ItemStack。
     * </p>
     *
     * @param newStack 新的 ItemStack 引用（当前玩家持有的实例）
     */
    public void updateStackReference(ItemStack newStack) {
        // 只更新引用，不重新加载状态
        // 因为状态已经通过 DataComponent 同步到新 ItemStack
        this.stack = newStack;
    }

    /**
     * 获取会话 ID
     */
    public UUID getSessionId() {
        return sessionId;
    }

    // ========== 状态加载/保存 ==========

    /**
     * 加载所有状态（物品栏、StateMachine、Owner、Tick 状态）
     */
    private void loadAllState() {
        CompoundTag dataTag = stack.get(RelayDataComponents.TOOL_SHELL_DATA);
        if (dataTag == null) {
            return;
        }

        // 加载物品栏
        loadInventory(dataTag);

        // 加载 StateMachine
        CompoundTag machineTag = dataTag.getCompound("stateMachine").orElse(null);
        if (machineTag != null) {
            StateMachineNbtSerializer.INSTANCE.deserialize(stateMachine, machineTag);
        }

        // 加载 Owner
        String uuidStr = dataTag.getString("owner").orElse("");
        if (!uuidStr.isEmpty()) {
            try {
                ownerUuid = java.util.UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                // UUID 格式错误，忽略
            }
        }

        // 加载 Tick 状态
        loadTickState();
    }

    /**
     * 保存所有状态
     * <p>
     * 公开访问，供 PlayerShellData 调用
     * </p>
     */
    public void saveAllState() {
        CompoundTag dataTag = new CompoundTag();

        // 保存物品栏
        saveInventory(dataTag);

        // 保存 StateMachine
        CompoundTag machineTag = StateMachineNbtSerializer.INSTANCE.serialize(stateMachine);
        dataTag.put("stateMachine", machineTag);

        // 保存 Owner
        if (owner != null) {
            dataTag.putString("owner", owner.getUUID().toString());
        }

        // 保存 Tick 状态
        saveTickState();

        stack.set(RelayDataComponents.TOOL_SHELL_DATA, dataTag);
    }

    // ========== 物品栏加载/保存 ==========

    /**
     * 从 CompoundTag 加载物品栏
     */
    private void loadInventory(CompoundTag tag) {
        ListTag listTag = tag.getList("inventory").orElse(null);
        if (listTag == null) {
            return;
        }

        ItemStack[] parsed = new ItemStack[inventory.size()];
        for (int i = 0; i < Math.min(parsed.length, listTag.size()); i++) {
            Tag element = listTag.get(i);
            if (element instanceof CompoundTag compoundTag) {
                var result = ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, compoundTag);
                parsed[i] = result.result().orElse(ItemStack.EMPTY);
            }
        }

        for (int i = 0; i < parsed.length; i++) {
            if (parsed[i] != null) {
                inventory.set(i, parsed[i]);
            }
        }
    }

    /**
     * 保存物品栏到 NBT
     */
    private void saveInventory(CompoundTag tag) {
        ListTag listTag = new ListTag();

        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                ItemStack.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, stack)
                        .result()
                        .ifPresent(listTag::add);
            } else {
                listTag.add(new CompoundTag());
            }
        }

        tag.put("inventory", listTag);
    }

    /**
     * 保存 StateMachine 到 DataComponent
     */
    public void saveStateMachine() {
        saveAllState();
    }

    /**
     * 从 DataComponent 加载 tick 状态
     */
    private void loadTickState() {
        CompoundTag stateTag = stack.get(RelayDataComponents.TOOL_SHELL_TICK_STATE);
        if (stateTag != null) {
            tickHandler.setTickCounter(stateTag.getInt("tickCounter").orElse(0));
            tickHandler.setInitialized(stateTag.getBoolean("initialized").orElse(false));
        }
    }

    /**
     * 保存 tick 状态到 DataComponent
     */
    private void saveTickState() {
        CompoundTag stateTag = stack.getOrDefault(RelayDataComponents.TOOL_SHELL_TICK_STATE, new CompoundTag());
        stateTag.putInt("tickCounter", tickHandler.getTickCounter());
        stateTag.putBoolean("initialized", tickHandler.isInitialized());
        stack.set(RelayDataComponents.TOOL_SHELL_TICK_STATE, stateTag);
    }

    // ========== Tick 逻辑 ==========

    /**
     * 执行 tick 逻辑
     * <p>
     * 不再每 tick 保存状态，由 PlayerShellData 管理保存时机
     * </p>
     */
    public void tick(Level world, Entity player) {
        // 设置 enabled 状态
        StateMachine machine = getStateMachine();
        if (machine.isRunning() && !isEnabled()) {
            setEnabled(true);
        }

        // 设置上下文
        if (machine.isRunning()) {
            machine.setContext("shellContainer", this);
            machine.setContext("level", world);
            machine.setContext("self", player);
        }

        // 执行 tick
        tickHandler.tick(this);
    }

    /**
     * 获取玩家实体（用于背包能量模块访问）
     *
     * @return 玩家实体，如果 owner 不是玩家返回 null
     */
    private net.minecraft.world.entity.player.Player getOwnerPlayer() {
        Entity owner = this.owner;
        return (owner instanceof net.minecraft.world.entity.player.Player)
                ? (net.minecraft.world.entity.player.Player) owner
                : null;
    }

    // ========== ShellContainer 接口 ==========

    @Override
    public StateMachine getStateMachine() {
        return stateMachine;
    }

    @Override
    public int getCoreCost() {
        ItemStack coreStack = getCoreStack();
        return !coreStack.isEmpty() ? coreStack.getCount() : 0;
    }

    @Override
    public int getInterval() {
        ItemStack coreStack = getCoreStack();
        if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
            return core.getInterval(coreStack);
        }
        return 0;
    }

    @Override
    public boolean isInitialized() {
        return tickHandler.isInitialized();
    }

    @Override
    public void setInitialized(boolean initialized) {
        tickHandler.setInitialized(initialized);
        saveTickState();
    }

    @Override
    public boolean isEnabled() {
        CompoundTag stateTag = stack.get(RelayDataComponents.TOOL_SHELL_TICK_STATE);
        if (stateTag == null) {
            return false;
        }
        return stateTag.getBoolean("enabled").orElse(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        CompoundTag stateTag = stack.getOrDefault(RelayDataComponents.TOOL_SHELL_TICK_STATE, new CompoundTag());
        stateTag.putBoolean("enabled", enabled);
        if (!enabled) {
            StateMachine machine = getStateMachine();
            machine.clear();
        }
        stack.set(RelayDataComponents.TOOL_SHELL_TICK_STATE, stateTag);
        saveTickState();
    }

    @Override
    public double getEnergy() {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            return emi.getStoredEnergy(energyStack);
        }
        // 如果启用背包能量模块且插槽为空，检查背包
        if (isUseInventoryEnergyModule()) {
            net.minecraft.world.entity.player.Player player = getOwnerPlayer();
            if (player != null) {
                double totalEnergy = 0.0;
                var inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack slot = inv.getItem(i);
                    if (!slot.isEmpty() && slot.getItem() instanceof EnergyModuleComponent emiSlot) {
                        totalEnergy += emiSlot.getStoredEnergy(slot);
                    }
                }
                return totalEnergy;
            }
        }
        return 0;
    }

    /**
     * 设置能量（仅用于内部同步）
     * 使用背包能量模块时，不直接设置能量值，而是通过 consumeEnergy/addEnergy 管理
     */
    @Override
    public void setEnergy(double energy) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            emi.setStoredEnergy(energyStack, energy);
        }
    }

    /**
     * 消耗能量
     *
     * @param amount 需要消耗的能量
     * @return 如果能量充足并成功扣除返回 true，否则返回 false
     */
    @Override
    public boolean consumeEnergy(double amount) {
        double currentEnergy = getEnergy();
        if (currentEnergy < amount) {
            return false;
        }
        
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            double consumed = emi.consumeEnergy(energyStack, amount);
            return consumed >= amount;
        }
        
        // 如果启用背包能量模块，从背包内的能量模块扣除
        net.minecraft.world.entity.player.Player player = getOwnerPlayer();
        if (player != null) {
            double remaining = amount;
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack slot = inv.getItem(i);
                if (!slot.isEmpty() && slot.getItem() instanceof EnergyModuleComponent emiSlot) {
                    double consumed = emiSlot.consumeEnergy(slot, remaining);
                    remaining -= consumed;
                    if (remaining <= 0) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    /**
     * 检查是否有足够能量
     */
    public boolean hasEnoughEnergy(double amount) {
        return getEnergy() >= amount;
    }

    @Override
    public void setChanged() {
        saveStateMachine();
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public Entity getOwner() {
        return owner;
    }

    @Override
    public void setOwner(Entity owner) {
        this.owner = owner;
        if (owner != null) {
            this.ownerUuid = owner.getUUID();
        }
        saveAllState();
    }

    @Override
    public boolean hasOwner() {
        if (this.owner == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean hasWorldInteractor() {
        if (inventory.get(INTERACTOR_SLOT).getItem() instanceof WorldInteractorComponent) {
            return true;
        }
        return false;
    }

    @Override
    public double getEnergyCostPerTick() {
        ItemStack coreStack = getCoreStack();
        if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreComponent core) {
            return core.getEnergyCost(coreStack);
        }
        return 0;
    }

    @Override
    public double addEnergy(double amount) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            return emi.addEnergy(energyStack, amount);
        }
        // 如果启用背包能量模块，添加到背包内的能量模块
        if (isUseInventoryEnergyModule()) {
            net.minecraft.world.entity.player.Player player = getOwnerPlayer();
            if (player != null) {
                double remaining = amount;
                var inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack slot = inv.getItem(i);
                    if (!slot.isEmpty() && slot.getItem() instanceof EnergyModuleComponent emiSlot) {
                        double added = emiSlot.addEnergy(slot, remaining);
                        remaining -= added;
                        if (remaining <= 0) {
                            return amount - remaining;
                        }
                    }
                }
                return amount - remaining;
            }
        }
        return 0;
    }

    @Override
    public void loadProgramFromDisk() {
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
            saveAllState();
        }
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

    // ========== 配置项 ==========

    public boolean isUseInventoryEnergyModule() {
        CompoundTag configTag = stack.get(RelayDataComponents.TOOL_SHELL_CONFIG);
        if (configTag == null) {
            return false;
        }
        return configTag.getBoolean("useInventoryEnergyModule").orElse(false);
    }

    public void setUseInventoryEnergyModule(boolean use) {
        CompoundTag configTag = stack.getOrDefault(RelayDataComponents.TOOL_SHELL_CONFIG, new CompoundTag());
        configTag.putBoolean("useInventoryEnergyModule", use);
        stack.set(RelayDataComponents.TOOL_SHELL_CONFIG, configTag);
    }

    public boolean isDebugOutputEnabled() {
        CompoundTag configTag = stack.get(RelayDataComponents.TOOL_SHELL_CONFIG);
        if (configTag == null) {
            return false;
        }
        return configTag.getBoolean("debugOutputEnabled").orElse(false);
    }

    public void setDebugOutputEnabled(boolean enabled) {
        CompoundTag configTag = stack.getOrDefault(RelayDataComponents.TOOL_SHELL_CONFIG, new CompoundTag());
        configTag.putBoolean("debugOutputEnabled", enabled);
        stack.set(RelayDataComponents.TOOL_SHELL_CONFIG, configTag);
    }

    // ========== Container 接口实现 ==========

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
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.set(i, ItemStack.EMPTY);
        }
    }


}
