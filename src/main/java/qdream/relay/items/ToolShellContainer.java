package qdream.relay.items;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import qdream.relay.Component.RelayDataComponents;
import qdream.relay.engine.StateMachine;
import qdream.relay.types.EntityType;
import qdream.relay.core.ShellContainer;
import qdream.relay.core.ShellStateManager;
import qdream.relay.core.ShellTickHandler;

import java.util.UUID;

/**
 * 工具外壳的 ShellContainer 实现
 *
 * <p>使用 {@link ShellStateManager} 统一管理物品栏、StateMachine、Owner 状态</p>
 *
 * <h3>存储结构</h3>
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

    final ToolShellItem toolShell; // package-private for ShellContainerWrapper
    final ItemStack stack; // package-private for ShellContainerWrapper
    private final UUID sessionId; // 会话 ID
    private final ShellStateManager stateManager;
    private final ShellTickHandler tickHandler = new ShellTickHandler();
    private Entity owner;

    public ToolShellContainer(ToolShellItem toolShell, ItemStack stack, UUID sessionId) {
        this.toolShell = toolShell;
        this.stack = stack;
        this.sessionId = sessionId;
        this.stateManager = new ShellStateManager();
        this.owner = null;

        loadAllState();

        // 设置事故回调
        StateMachine machine = getStateMachine();
        machine.setMishapHandler(reason -> {
            if (owner != null && owner instanceof net.minecraft.world.entity.player.Player player) {
                player.sendSystemMessage(Component.literal("§c[工具外壳] 事故：" + reason));
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

        // 加载物品栏和 StateMachine、Owner
        stateManager.loadFromTag(dataTag);

        // 加载 Tick 状态
        loadTickState();
    }

    /**
     * 保存所有状态
     * <p>公开访问，供 PlayerShellData 调用</p>
     */
    public void saveAllState() {
        // 保存物品栏、StateMachine、Owner 到 TOOL_SHELL_DATA
        CompoundTag dataTag = stateManager.saveToTag();
        
        // 保存 Tick 状态
        saveTickState();
        
        stack.set(RelayDataComponents.TOOL_SHELL_DATA, dataTag);
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
     * <p>不再每 tick 保存状态，由 PlayerShellData 管理保存时机</p>
     */
    public void tick(Level world, Entity player) {
        this.owner = player;

        // 设置 enabled 状态
        StateMachine machine = getStateMachine();
        if (machine.isRunning() && !isEnabled()) {
            setEnabled(true);
        }

        // 设置上下文
        if (machine.isRunning()) {
            machine.setContext("worldInteractor", getInteractorStack());
            machine.setContext("shellContainer", this);
            machine.setContext("world", world);
            machine.setContext("self",
                    new EntityType(player.getUUID(), player.level().dimension().identifier().toString(), player));
        }

        // 执行 tick
        tickHandler.tick(this);
    }

    // ========== ShellContainer 接口 ==========

    @Override
    public ItemStack getInventorySlot(int slot) {
        return stateManager.getInventorySlot(slot);
    }

    @Override
    public void setInventorySlot(int slot, ItemStack itemStack) {
        stateManager.setInventorySlot(slot, itemStack);
        saveAllState();
    }

    @Override
    public StateMachine getStateMachine() {
        return stateManager.getStateMachine();
    }

    @Override
    public int getCoreCount() {
        ItemStack coreStack = getCoreStack();
        return !coreStack.isEmpty() ? coreStack.getCount() : 0;
    }

    @Override
    public int getInterval() {
        ItemStack coreStack = getCoreStack();
        if (!coreStack.isEmpty() && coreStack.getItem() instanceof ComputingCoreItem coreItem) {
            return coreItem.getInterval(coreStack);
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
        return stateTag.getBoolean("initialized").orElse(false);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled) {
            StateMachine machine = getStateMachine();
            machine.clear();
            saveStateMachine();
        }
    }

    @Override
    public double getEnergy() {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleItem) {
            return EnergyModuleItem.getStoredEnergy(energyStack);
        }
        if (isUseInventoryEnergyModule()) {
            // TODO: 实现背包能量模块检查
        }
        return 0;
    }

    @Override
    public void setEnergy(double energy) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleItem) {
            EnergyModuleItem.setStoredEnergy(energyStack, energy);
        }
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
        stateManager.setOwner(owner);
        saveAllState();
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

    // ========== 快捷方法 ==========

    public ItemStack getCoreStack() {
        return getInventorySlot(CORE_SLOT);
    }

    public ItemStack getDiskStack() {
        return getInventorySlot(DISK_SLOT);
    }

    public ItemStack getEnergyStack() {
        return getInventorySlot(ENERGY_SLOT);
    }

    public ItemStack getInteractorStack() {
        return getInventorySlot(INTERACTOR_SLOT);
    }
}
