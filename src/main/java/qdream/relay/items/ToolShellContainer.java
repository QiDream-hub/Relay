package qdream.relay.items;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;

import qdream.relay.engine.StateMachine;
import qdream.relay.core.ShellContainer;

/**
 * 工具外壳的 ShellContainer 实现
 * 使用 ItemStack 的 DataComponent 或 NBT 存储状态
 */
public class ToolShellContainer implements ShellContainer {

    private final ToolShellItem toolShell;
    private final ItemStack stack;
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
    }

    @Override
    public StateMachine getStateMachine() {
        return toolShell.getStateMachine(stack);
    }

    @Override
    public int getCoreCount() {
        return 1;
    }

    @Override
    public int getInterval() {
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
        return 0;
    }

    @Override
    public void setEnergy(double energy) {
        // 工具外壳的能量存储在 ItemStack 中
    }

    @Override
    public void setChanged() {
        // 物品不需要
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
}
