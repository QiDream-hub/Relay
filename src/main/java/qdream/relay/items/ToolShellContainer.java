package qdream.relay.items;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import qdream.relay.engine.StateMachine;
import qdream.relay.types.EntityType;
import qdream.relay.core.ShellContainer;
import qdream.relay.core.ShellTickHandler;

/**
 * 工具外壳的 ShellContainer 实现
 * 委托给 ToolShellItem 使用 DataComponent 存储状态
 * 
 * 注意：Tick 状态（tickCounter, initialized）持久化在 ItemStack 的 DataComponent 中
 * 每 tick 创建新实例时从 DataComponent 加载状态
 */
public class ToolShellContainer implements ShellContainer {

    final ToolShellItem toolShell; // package-private for ShellContainerWrapper
    final ItemStack stack; // package-private for ShellContainerWrapper
    private Entity owner;
    private final ShellTickHandler tickHandler = new ShellTickHandler();

    public ToolShellContainer(ToolShellItem toolShell, ItemStack stack) {
        this.toolShell = toolShell;
        this.stack = stack;
        this.owner = null;

        // 从 DataComponent 加载 tick 状态
        loadTickState();

        // 设置事故回调 - 给玩家发送消息
        StateMachine machine = getStateMachine();
        machine.setMishapHandler(reason -> {
            if (owner != null && owner instanceof net.minecraft.world.entity.player.Player player) {
                player.sendSystemMessage(Component.literal("§c[工具外壳] 事故：" + reason));
            }
        });
    }

    /**
     * 从 DataComponent 加载 tick 状态到 tickHandler
     */
    private void loadTickState() {
        int tickCounter = toolShell.getTickCounter(stack);
        boolean initialized = toolShell.isInitialized(stack);
        tickHandler.setTickCounter(tickCounter);
        tickHandler.setInitialized(initialized);
    }

    /**
     * 保存 tick 状态到 DataComponent
     */
    private void saveTickState() {
        toolShell.saveTickState(stack, tickHandler.getTickCounter(), tickHandler.isInitialized());
    }

    /**
     * 执行 tick 逻辑
     * 需要在玩家 inventory tick 时调用
     */
    public void tick(Level world, Entity player) {
        // 设置 owner
        this.owner = player;

        // 设置 enabled 状态（从 StateMachine 运行状态推导）
        StateMachine machine = getStateMachine();
        if (machine.isRunning() && !isEnabled()) {
            setEnabled(true);
        }

        // tick 后设置上下文（供操作使用）
        if (machine.isRunning()) {
            machine.setContext("worldInteractor", getInteractorStack());
            machine.setContext("shellContainer", this);
            machine.setContext("world", world);
            machine.setContext("self",
                    new EntityType(player.getUUID(), player.level().dimension().identifier().toString(), player));
        }

        // 使用 ShellTickHandler 执行 tick
        tickHandler.tick(this);

        // 保存 tick 状态到 DataComponent
        saveTickState();
    }

    @Override
    public ItemStack getInventorySlot(int slot) {
        return toolShell.getInventorySlot(stack, slot);
    }

    @Override
    public void setInventorySlot(int slot, ItemStack itemStack) {
        toolShell.setInventorySlot(stack, slot, itemStack);
        // 物品栏变更时，保存 StateMachine 状态
        StateMachine machine = getStateMachine();
        if (machine.isRunning()) {
            toolShell.saveStateMachine(stack, machine);
        }
    }

    @Override
    public StateMachine getStateMachine() {
        return toolShell.getStateMachine(stack);
    }

    @Override
    public int getCoreCount() {
        // 从核心物品读取核心数量（堆叠数量 = 并行度）
        ItemStack coreStack = getCoreStack();
        if (!coreStack.isEmpty()) {
            return coreStack.getCount();
        }
        return 0;
    }

    @Override
    public int getInterval() {
        // 从核心物品读取 interval 属性
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
        // 同时保存到 DataComponent
        toolShell.saveTickState(stack, tickHandler.getTickCounter(), initialized);
    }

    @Override
    public boolean isEnabled() {
        return toolShell.isEnabled(stack);
    }

    @Override
    public void setEnabled(boolean enabled) {
        toolShell.setEnabled(stack, enabled);
    }

    @Override
    public double getEnergy() {
        // 工具外壳的能量从能量模块读取
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleItem) {
            return EnergyModuleItem.getStoredEnergy(energyStack);
        }
        // 如果启用了背包能量模块，检查玩家背包
        if (isUseInventoryEnergyModule()) {
            // TODO: 实现背包能量模块检查
        }
        return 0;
    }

    @Override
    public void setEnergy(double energy) {
        // 工具外壳的能量存储在能量模块中
        ItemStack energyStack = getEnergyStack();
        if (!energyStack.isEmpty() && energyStack.getItem() instanceof EnergyModuleItem) {
            EnergyModuleItem.setStoredEnergy(energyStack, energy);
        }
    }

    @Override
    public void setChanged() {
        // 物品不需要标记变更，但需要保存 StateMachine 状态
        StateMachine machine = getStateMachine();
        if (machine.isRunning()) {
            toolShell.saveStateMachine(stack, machine);
        }
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
    }

    // ========== 配置项 ==========

    /**
     * 获取是否使用背包内的能量模块
     */
    public boolean isUseInventoryEnergyModule() {
        return toolShell.isUseInventoryEnergyModule(stack);
    }

    /**
     * 设置是否使用背包内的能量模块
     */
    public void setUseInventoryEnergyModule(boolean use) {
        toolShell.setUseInventoryEnergyModule(stack, use);
    }

    // ========== 快捷方法 ==========

    public ItemStack getCoreStack() {
        return getInventorySlot(ToolShellItem.CORE_SLOT);
    }

    public ItemStack getDiskStack() {
        return getInventorySlot(ToolShellItem.DISK_SLOT);
    }

    public ItemStack getEnergyStack() {
        return getInventorySlot(ToolShellItem.ENERGY_SLOT);
    }

    public ItemStack getInteractorStack() {
        return getInventorySlot(ToolShellItem.INTERACTOR_SLOT);
    }
}
