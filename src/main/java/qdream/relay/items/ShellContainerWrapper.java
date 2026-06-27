package qdream.relay.items;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import qdream.relay.engine.StateMachine;
import qdream.relay.core.ShellContainer;

/**
 * ShellContainer 的包装器，实现 Container 接口用于 GUI 插槽
 * 注意：26.1.2 使用 DataComponent 系统，这里暂时简化实现
 */
public class ShellContainerWrapper implements Container {

    private final ShellContainer delegate;

    /**
     * 用于工具外壳的包装器
     */
    public ShellContainerWrapper(ToolShellItem toolShell, ItemStack stack) {
        this.delegate = new ToolShellContainer(toolShell, stack);
    }

    /**
     * 用于实体外壳/方块外壳的包装器（直接使用 ShellContainer 作为 delegate）
     */
    public ShellContainerWrapper(ShellContainer delegate) {
        this.delegate = delegate;
    }

    // Container 接口方法

    @Override
    public int getContainerSize() {
        return 4; // 4 个插槽
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < 4; i++) {
            if (!getInventorySlot(i).isEmpty()) {
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
        ItemStack stack = getInventorySlot(slot);
        if (!stack.isEmpty()) {
            ItemStack result = stack.split(amount);
            setInventorySlot(slot, stack);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getInventorySlot(slot);
        if (!stack.isEmpty()) {
            setInventorySlot(slot, ItemStack.EMPTY);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setInventorySlot(slot, stack);
    }

    @Override
    public void setChanged() {
        if (delegate != null) {
            delegate.setChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < 4; i++) {
            setInventorySlot(i, ItemStack.EMPTY);
        }
    }

    // ShellContainer 接口方法（委托）

    public ItemStack getInventorySlot(int slot) {
        if (delegate != null) {
            return delegate.getInventorySlot(slot);
        }
        return ItemStack.EMPTY;
    }

    public void setInventorySlot(int slot, ItemStack itemStack) {
        if (delegate != null) {
            delegate.setInventorySlot(slot, itemStack);
        }
    }

    public StateMachine getStateMachine() {
        if (delegate != null) {
            return delegate.getStateMachine();
        }
        return new StateMachine(1024);
    }

    public int getCoreCount() {
        if (delegate != null) {
            return delegate.getCoreCount();
        }
        return 0;
    }

    public int getInterval() {
        if (delegate != null) {
            return delegate.getInterval();
        }
        return 1;
    }

    public boolean isInitialized() {
        if (delegate != null) {
            return delegate.isInitialized();
        }
        return false;
    }

    public void setInitialized(boolean initialized) {
        if (delegate != null) {
            delegate.setInitialized(initialized);
        }
    }

    public boolean isEnabled() {
        if (delegate != null) {
            return delegate.isEnabled();
        }
        return false;
    }

    public void setEnabled(boolean enabled) {
        if (delegate != null) {
            delegate.setEnabled(enabled);
        }
    }

    public double getEnergy() {
        if (delegate != null) {
            return delegate.getEnergy();
        }
        return 0;
    }

    public void setEnergy(int energy) {
        if (delegate != null) {
            delegate.setEnergy(energy);
        }
    }

    public boolean isClientSide() {
        if (delegate != null) {
            return delegate.isClientSide();
        }
        return false;
    }
}
