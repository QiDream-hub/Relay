package qdream.relay.items.container;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

import qdream.relay.Component.RelayDataComponents;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.ToolShellItem;
import qdream.relay.types.EntityData;
import qdream.relay.core.ShellContainer;
import qdream.relay.core.ShellStateManager;
import qdream.relay.core.ShellTickHandler;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.mc.component.WorldInteractorComponent;

import java.util.UUID;

/**
 * 工具外壳的 ShellContainer 实现
 *
 * <p>
 * 使用 {@link ShellStateManager} 统一管理物品栏、StateMachine、Owner 状态
 * </p>
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
public class ToolShellContainer implements ShellContainer, Container {

    public static final int CORE_SLOT = 0;
    public static final int DISK_SLOT = 1;
    public static final int ENERGY_SLOT = 2;
    public static final int INTERACTOR_SLOT = 3;

    final ToolShellItem toolShell; // package-private for direct access
    ItemStack stack; // package-private - 非 final 以支持引用更新
    private final UUID sessionId; // 会话 ID
    private final ShellStateManager stateManager;
    private final ShellTickHandler tickHandler = new ShellTickHandler();

    public ToolShellContainer(ToolShellItem toolShell, ItemStack stack, UUID sessionId) {
        this.toolShell = toolShell;
        this.stack = stack;
        this.sessionId = sessionId;
        this.stateManager = new ShellStateManager();

        loadAllState();

        // 设置事故回调
        StateMachine machine = getStateMachine();
        machine.setMishapHandler(reason -> {
            Entity owner = stateManager.getOwner();
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

        // 加载物品栏和 StateMachine、Owner
        stateManager.loadFromTag(dataTag);

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
        Entity owner = stateManager.getOwner();
        return (owner instanceof net.minecraft.world.entity.player.Player)
                ? (net.minecraft.world.entity.player.Player) owner
                : null;
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

    @Override
    public void setEnergy(double energy) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            emi.setStoredEnergy(energyStack, energy);
        }
        // 使用背包能量模块时，不直接设置能量值，而是通过 consumeEnergy/addEnergy 管理
    }

    /**
     * 消耗能量
     * 
     * @param amount 需要消耗的能量
     * @return 实际消耗的能量
     */
    public double consumeEnergy(double amount) {
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleComponent emi) {
            return emi.consumeEnergy(energyStack, amount);
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
                        return amount;
                    }
                }
            }
            return amount - remaining;
        }
        return 0;
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
        return stateManager.getOwner();
    }

    @Override
    public void setOwner(Entity owner) {
        stateManager.setOwner(owner);
        saveAllState();
    }

    @Override
    public boolean hasOwner() {
        if (stateManager.getOwner() == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean hasWorldInteractor() {
        if (stateManager.getInventorySlot(INTERACTOR_SLOT).getItem() instanceof WorldInteractorComponent) {
            return true;
        }
        return false;
    }

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
            setInventorySlot(slot, stack);
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
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < 4; i++) {
            stateManager.setInventorySlot(i, ItemStack.EMPTY);
        }
    }

}
