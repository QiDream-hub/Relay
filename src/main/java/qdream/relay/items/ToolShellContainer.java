package qdream.relay.items;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import qdream.relay.engine.StateMachine;
import qdream.relay.core.ShellContainer;

/**
 * 工具外壳的 ShellContainer 实现
 * 委托给 ToolShellItem 使用 DataComponent 存储状态
 */
public class ToolShellContainer implements ShellContainer {

    final ToolShellItem toolShell;  // package-private for ShellContainerWrapper
    final ItemStack stack;  // package-private for ShellContainerWrapper
    private Entity owner;

    public ToolShellContainer(ToolShellItem toolShell, ItemStack stack) {
        this.toolShell = toolShell;
        this.stack = stack;
        this.owner = null;
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
        // 工具外壳固定为 1 个核心
        return 1;
    }

    @Override
    public int getInterval() {
        // 工具外壳每 tick 执行一次
        return 1;
    }

    @Override
    public boolean isInitialized() {
        return toolShell.isInitialized(stack);
    }

    @Override
    public void setInitialized(boolean initialized) {
        toolShell.setInitialized(stack, initialized);
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
        // 物品不需要标记变更，但需要保存 StateMachine
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
