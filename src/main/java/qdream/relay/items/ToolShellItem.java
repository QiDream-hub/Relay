package qdream.relay.items;

import java.util.List;
import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;

import qdream.relay.Component.RelayDataComponents;
import qdream.relay.commands.CommandUtils;
import qdream.relay.engine.StateMachine;
import qdream.relay.engine.Executable;
import qdream.relay.mc.StateMachineNbtSerializer;

/**
 * 工具外壳（手持物品形态）
 * 手持右键激活程序，在物品栏中持续运行
 * StateMachine 为运行状态的绝对权威 - 程序栈非空 = 运行中
 */
public class ToolShellItem extends Item {

    // 插槽索引常量（与 ShellContainer 保持一致）
    public static final int CORE_SLOT = 0;
    public static final int DISK_SLOT = 1;
    public static final int ENERGY_SLOT = 2;
    public static final int INTERACTOR_SLOT = 3;
    private static final int SLOT_COUNT = 4;

    public ToolShellItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    // ========== 右键交互逻辑 ==========

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        StateMachine machine = getStateMachine(stack);

        // Shift+ 右键：停止程序（清空双栈）
        if (player.isShiftKeyDown()) {
            machine.clear();
            saveStateMachine(stack, machine);
            player.sendSystemMessage(Component.literal("§c[工具外壳] 程序已停止"));
            return InteractionResult.SUCCESS;
        }

        // 普通右键：检查运行状态
        if (machine.isRunning()) {
            // 正在运行中，提示用户
            player.sendSystemMessage(Component.literal("§e[工具外壳] 程序正在运行中"));
            player.sendSystemMessage(
                    Component.literal("§e[程序栈] " + CommandUtils.dataStackToString(machine.getProgramStackSnapshot())));
            player.sendSystemMessage(
                    Component.literal("§e[数据序栈] " + CommandUtils.dataStackToString(machine.getDataStackSnapshot())));
        } else {
            // 已停止，加载磁盘程序并运行
            ItemStack diskStack = getInventorySlot(stack, DISK_SLOT);
            if (!diskStack.isEmpty() && diskStack.getItem() instanceof SpellDiskItem) {
                List<Executable> program = SpellDiskItem.getProgram(diskStack);
                if (!program.isEmpty()) {
                    // 清空双栈后加载新程序
                    machine.clear();
                    machine.loadProgram(program);
                    saveStateMachine(stack, machine);
                    // 设置初始化状态（与 ShellBlockEntity 保持一致）
                    setInitialized(stack, true);
                    player.sendSystemMessage(Component.literal("§a[工具外壳] 程序已启动，共 " + program.size() + " 个指令"));
                } else {
                    player.sendSystemMessage(Component.literal("§e[工具外壳] 磁盘为空，无法启动"));
                }
            } else {
                player.sendSystemMessage(Component.literal("§e[工具外壳] 未插入法术磁盘"));
            }
        }

        // 设置 Owner（每次右键时更新）
        setOwner(stack, player);

        return InteractionResult.SUCCESS;
    }

    // ========== 物品栏插槽访问（使用 Codec 序列化） ==========

    /**
     * 获取指定插槽的物品
     */
    public ItemStack getInventorySlot(ItemStack shell, int slot) {
        CompoundTag dataTag = shell.get(RelayDataComponents.TOOL_SHELL_DATA);
        if (dataTag == null) {
            return ItemStack.EMPTY;
        }

        // 使用 NonNullList 存储物品栏
        NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        loadInventory(dataTag, inventory);

        if (slot >= 0 && slot < inventory.size()) {
            return inventory.get(slot);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 设置指定插槽的物品
     */
    public void setInventorySlot(ItemStack shell, int slot, ItemStack stack) {
        CompoundTag dataTag = shell.getOrDefault(RelayDataComponents.TOOL_SHELL_DATA, new CompoundTag());

        // 加载现有物品栏
        NonNullList<ItemStack> inventory = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        loadInventory(dataTag, inventory);

        // 设置插槽物品
        if (slot >= 0 && slot < inventory.size()) {
            inventory.set(slot, stack);
        }

        // 保存物品栏
        saveInventory(dataTag, inventory);
        shell.set(RelayDataComponents.TOOL_SHELL_DATA, dataTag);
    }

    /**
     * 从 NBT 加载物品栏
     */
    private void loadInventory(CompoundTag tag, NonNullList<ItemStack> inventory) {
        ListTag listTag = tag.getList("inventory").orElse(null);
        if (listTag == null) {
            return;
        }

        // 先解析所有物品到临时数组
        ItemStack[] parsed = new ItemStack[inventory.size()];
        for (int i = 0; i < Math.min(parsed.length, listTag.size()); i++) {
            Tag element = listTag.get(i);
            if (element instanceof CompoundTag compoundTag) {
                // 使用 Codec 解析 ItemStack
                var result = ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, compoundTag);
                parsed[i] = result.result().orElse(ItemStack.EMPTY);
            }
        }

        // 然后复制到 inventory
        for (int i = 0; i < parsed.length; i++) {
            if (parsed[i] != null) {
                inventory.set(i, parsed[i]);
            }
        }
    }

    /**
     * 保存物品栏到 NBT
     */
    private void saveInventory(CompoundTag tag, NonNullList<ItemStack> inventory) {
        ListTag listTag = new ListTag();

        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                // 使用 Codec 序列化 ItemStack
                ItemStack.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, stack)
                        .result()
                        .ifPresent(listTag::add);
            } else {
                // 空物品存储为空 CompoundTag
                listTag.add(new CompoundTag());
            }
        }

        tag.put("inventory", listTag);
    }

    // ========== 快捷访问方法 ==========

    public ItemStack getCoreStack(ItemStack shell) {
        return getInventorySlot(shell, CORE_SLOT);
    }

    public ItemStack getDiskStack(ItemStack shell) {
        return getInventorySlot(shell, DISK_SLOT);
    }

    public ItemStack getEnergyStack(ItemStack shell) {
        return getInventorySlot(shell, ENERGY_SLOT);
    }

    public ItemStack getInteractorStack(ItemStack shell) {
        return getInventorySlot(shell, INTERACTOR_SLOT);
    }

    // ========== StateMachine 持久化 ==========

    /**
     * 获取 StateMachine（从 DataComponent 加载）
     */
    public StateMachine getStateMachine(ItemStack shell) {
        StateMachine machine = new StateMachine(1024);
        CompoundTag dataTag = shell.get(RelayDataComponents.TOOL_SHELL_DATA);

        if (dataTag != null) {
            // 加载状态机状态
            CompoundTag machineTag = dataTag.getCompound("stateMachine").orElse(null);
            if (machineTag != null) {
                StateMachineNbtSerializer.INSTANCE.deserialize(machine, machineTag);
            }
        }

        return machine;
    }

    /**
     * 保存 StateMachine 到 DataComponent
     */
    public void saveStateMachine(ItemStack shell, StateMachine machine) {
        CompoundTag dataTag = shell.getOrDefault(RelayDataComponents.TOOL_SHELL_DATA, new CompoundTag());

        // 保存状态机状态
        CompoundTag machineTag = StateMachineNbtSerializer.INSTANCE.serialize(machine);
        dataTag.put("stateMachine", machineTag);

        shell.set(RelayDataComponents.TOOL_SHELL_DATA, dataTag);
    }

    // ========== 状态查询 ==========

    /**
     * 检查是否正在运行（程序栈非空）
     */
    public boolean isRunning(ItemStack shell) {
        StateMachine machine = getStateMachine(shell);
        return machine.isRunning();
    }

    /**
     * 检查是否已初始化（至少有一个插槽有物品）
     * 注意：此方法用于检查物品栏是否已配置，而非执行器的 initialized 状态
     */
    public boolean hasInventoryItems(ItemStack shell) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!getInventorySlot(shell, i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取执行器的 initialized 状态（从 tick 状态中读取）
     */
    public boolean isInitialized(ItemStack shell) {
        CompoundTag stateTag = getTickState(shell);
        return stateTag.getBoolean("initialized").orElse(false);
    }

    /**
     * 设置执行器的 initialized 状态
     */
    public void setInitialized(ItemStack shell, boolean initialized) {
        CompoundTag stateTag = getTickState(shell);
        stateTag.putBoolean("initialized", initialized);
        shell.set(RelayDataComponents.TOOL_SHELL_TICK_STATE, stateTag);
    }

    public boolean isEnabled(ItemStack shell) {
        return isRunning(shell);
    }

    public void setEnabled(ItemStack shell, boolean enabled) {
        if (!enabled) {
            StateMachine machine = getStateMachine(shell);
            machine.clear();
            saveStateMachine(shell, machine);
        }
    }

    // ========== Owner 管理 ==========

    /**
     * 设置工具外壳的拥有者
     */
    public void setOwner(ItemStack shell, net.minecraft.world.entity.Entity owner) {
        CompoundTag dataTag = shell.getOrDefault(RelayDataComponents.TOOL_SHELL_DATA, new CompoundTag());

        if (owner != null) {
            dataTag.putString("owner", owner.getUUID().toString());
        }

        shell.set(RelayDataComponents.TOOL_SHELL_DATA, dataTag);
    }

    /**
     * 获取工具外壳的拥有者
     */
    public net.minecraft.world.entity.Entity getOwner(ItemStack shell, Level world) {
        CompoundTag dataTag = shell.get(RelayDataComponents.TOOL_SHELL_DATA);
        if (dataTag == null) {
            return null;
        }

        String uuidStr = dataTag.getString("owner").orElse("");
        if (uuidStr.isEmpty() || world == null) {
            return null;
        }

        try {
            java.util.UUID ownerUuid = java.util.UUID.fromString(uuidStr);
            // 延迟加载实体
            return world.getEntity(ownerUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ========== 配置项 ==========

    /**
     * 获取是否使用背包内的能量模块
     */
    public boolean isUseInventoryEnergyModule(ItemStack shell) {
        CompoundTag configTag = shell.get(RelayDataComponents.TOOL_SHELL_CONFIG);
        if (configTag == null) {
            return false;
        }
        return configTag.getBoolean("useInventoryEnergyModule").orElse(false);
    }

    /**
     * 设置是否使用背包内的能量模块
     */
    public void setUseInventoryEnergyModule(ItemStack shell, boolean use) {
        CompoundTag configTag = shell.getOrDefault(RelayDataComponents.TOOL_SHELL_CONFIG, new CompoundTag());
        configTag.putBoolean("useInventoryEnergyModule", use);
        shell.set(RelayDataComponents.TOOL_SHELL_CONFIG, configTag);
    }

    // ========== Tick 状态管理 ==========

    /**
     * 获取 Tick 状态（tickCounter, initialized）
     */
    public CompoundTag getTickState(ItemStack shell) {
        CompoundTag stateTag = shell.get(RelayDataComponents.TOOL_SHELL_TICK_STATE);
        if (stateTag == null) {
            CompoundTag newState = new CompoundTag();
            newState.putInt("tickCounter", 0);
            newState.putBoolean("initialized", false);
            shell.set(RelayDataComponents.TOOL_SHELL_TICK_STATE, newState);
            return newState;
        }
        return stateTag;
    }

    /**
     * 保存 Tick 状态
     */
    public void saveTickState(ItemStack shell, int tickCounter, boolean initialized) {
        CompoundTag stateTag = shell.getOrDefault(RelayDataComponents.TOOL_SHELL_TICK_STATE, new CompoundTag());
        stateTag.putInt("tickCounter", tickCounter);
        stateTag.putBoolean("initialized", initialized);
        shell.set(RelayDataComponents.TOOL_SHELL_TICK_STATE, stateTag);
    }

    /**
     * 获取 tick 计数器
     */
    public int getTickCounter(ItemStack shell) {
        CompoundTag stateTag = getTickState(shell);
        return stateTag.getInt("tickCounter").orElse(0);
    }

    /**
     * 设置 tick 计数器
     */
    public void setTickCounter(ItemStack shell, int counter) {
        CompoundTag stateTag = getTickState(shell);
        stateTag.putInt("tickCounter", counter);
        shell.set(RelayDataComponents.TOOL_SHELL_TICK_STATE, stateTag);
    }
}
