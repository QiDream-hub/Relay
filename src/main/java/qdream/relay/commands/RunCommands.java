package qdream.relay.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.DiskItem;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.mc.component.WorldInteractorComponent;
import qdream.relay.mc.component.ComputingCoreComponent;
import qdream.relay.mc.component.DiskComponent;

/**
 * 运行法术命令
 *
 * 命令格式：
 * /relay run hand - 默认配置 (1000 能量，世界交互器范围 16)
 * /relay run hand <energy> - 自定义能量 (-1=无限)
 * /relay run hand <energy> <range> - 世界交互器范围
 */
public class RunCommands {
    /**
     * 注册运行命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            LiteralCommandNode<CommandSourceStack> root) {
        // /relay run hand [energy] [range]
        var energyNode = Commands.argument("energy", DoubleArgumentType.doubleArg(-1, 100000))
                .executes(RunCommands::runHandWithEnergy)
                .then(Commands.argument("range", DoubleArgumentType.doubleArg(0, 128))
                        .executes(ctx -> runHandWithInteractor(ctx,
                                DoubleArgumentType.getDouble(ctx, "energy"))));

        // /relay run hand
        var handNode = Commands.literal("hand")
                .executes(RunCommands::runHand)
                .then(energyNode)
                .build();

        // /relay run
        var runNode = Commands.literal("run")
                .then(handNode)
                .build();

        root.addChild(runNode);
    }

    // 错误定义
    private static final SimpleCommandExceptionType NO_DISK = new SimpleCommandExceptionType(
            Component.literal("手中没有法术磁盘"));

    /**
     * 一次性执行所有步骤，不进行 tick 分割
     *
     * @param machine      状态机
     * @param energyModule 能量模块
     * @return 执行的操作数
     */
    private static int executeAll(StateMachine machine, ItemStack energyModule) {
        int executedCount = 0;

        // 持续执行直到程序栈为空或发生事故
        while (machine.isRunning()) {
            if (!machine.step()) {
                break;
            }
            executedCount++;

            // 安全检查：防止无限循环
            if (executedCount > 100000) {
                machine.triggerMishap("操作数超过限制");
                break;
            }
        }

        return executedCount;
    }

    /**
     * 运行手中的法术磁盘（基础版）
     * 默认：1000 能量，世界交互器范围 16
     */
    private static int runHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return runHandInternal(context, 1000.0, 16.0);
    }

    /**
     * 运行手中的法术磁盘（自定义能量）
     */
    private static int runHandWithEnergy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        double energy = DoubleArgumentType.getDouble(context, "energy");
        return runHandInternal(context, energy, 16.0);
    }

    /**
     * 运行手中的法术磁盘（完整参数）
     */
    private static int runHandWithInteractor(CommandContext<CommandSourceStack> context, double energy)
            throws CommandSyntaxException {
        double range = DoubleArgumentType.getDouble(context, "range");
        return runHandInternal(context, energy, range);
    }

    /**
     * 从物品堆获取 SpellDiskComponent
     */
    private static DiskComponent getDiskComponent(ItemStack stack) {
        if (stack.getItem() instanceof DiskComponent) {
            return (DiskComponent) stack.getItem();
        }
        return null;
    }

    /**
     * 内部统一执行方法
     */
    private static int runHandInternal(CommandContext<CommandSourceStack> context,
            double energy, double range) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayerOrException();
        ItemStack diskStack = player.getMainHandItem();

        if (!(diskStack.getItem() instanceof DiskItem)) {
            throw NO_DISK.create();
        }

        // 创建临时物品
        ItemStack energyModule = new ItemStack(new EnergyModule(new Item.Properties()));
        ItemStack worldInteractor = new ItemStack(new WorldInteractor(new Item.Properties()));
        ItemStack computingCore = new ItemStack(new ComputingCore(new Item.Properties()));

        // 配置能量
        if (energyModule.getItem() instanceof EnergyModule em) {
            if (energy < 0) {
                em.setIsUnlimitedEnergy(true);
            } else {
                em.setStoredEnergy(energyModule, energy);
            }
        }

        // 配置世界交互器范围
        if (worldInteractor.getItem() instanceof WorldInteractor wi) {
            wi.setRange(worldInteractor, range);
        }

        // 创建状态机
        StateMachine stateMachine = new StateMachine();

        // 从磁盘加载程序
        DiskComponent diskComponent = getDiskComponent(diskStack);
        if (diskComponent != null) {
            List<Executable> program = diskComponent.getProgram(diskStack);
            stateMachine.loadProgram(program);
        }

        // 创建 Shell 容器
        CommandShellContainer commandShellContainer = new CommandShellContainer(
                stateMachine, player, computingCore, energyModule, worldInteractor, diskStack);
        commandShellContainer.setOwner(player);

        // 设置执行上下文
        stateMachine.setContext("self", player);
        stateMachine.setContext("level", source.getLevel());
        stateMachine.setContext("shellContainer", commandShellContainer);

        stateMachine.setMishapHandler(e -> {
            player.sendSystemMessage(Component.literal(e));
        });

        // 记录初始能量
        EnergyModule em = (EnergyModule) energyModule.getItem();

        // 一次性执行所有步骤
        int executedCount = executeAll(stateMachine, energyModule);

        // 计算消耗
        double consumedEnergy = em.getConsumeEnergy();
        double addedEnergy = em.getAddEnergy();
        double remainingEnergy = em.getStoredEnergy(energyModule);

        // 输出结果
        player.sendSystemMessage(Component.literal(
                "§a=== 法术执行完成 ===§r\n" +
                        "执行步数：" + executedCount + "\n" +
                        "消耗能量：" + consumedEnergy + "\n" +
                        "添加能量：" + addedEnergy + "\n" +
                        "剩余能量：" + remainingEnergy));

        return executedCount;
    }

    // 定义各组件
    static class EnergyModule extends Item implements EnergyModuleComponent {

        double energy;
        double addEnergy;
        double consumeEnergy;

        // 是否无限能量
        boolean isUnlimitedEnergy;

        public EnergyModule(Properties properties) {
            super(properties);
        }

        @Override
        public double getStoredEnergy(ItemStack stack) {
            return energy;
        }

        @Override
        public void setStoredEnergy(ItemStack stack, double energy) {
            this.energy = energy;
        }

        @Override
        public double addEnergy(ItemStack stack, double amount) {
            addEnergy += amount;
            energy += amount;
            return amount;
        }

        @Override
        public double consumeEnergy(ItemStack stack, double amount) {
            energy -= amount;
            consumeEnergy += amount;
            return amount;
        }

        @Override
        public boolean hasEnergy(ItemStack stack, double amount) {
            if (isUnlimitedEnergy) {
                return true;
            }
            if (energy < amount) {
                return false;
            }
            return true;
        }

        public void setIsUnlimitedEnergy(boolean isUnlimitedEnergy) {
            this.isUnlimitedEnergy = isUnlimitedEnergy;
        }

        public double getAddEnergy() {
            return addEnergy;
        }

        public double getConsumeEnergy() {
            return consumeEnergy;
        }

    }

    static class WorldInteractor extends Item implements WorldInteractorComponent {

        double range;
        double energyCost;

        public WorldInteractor(Properties properties) {
            super(properties);
        }

        @Override
        public double getRange(ItemStack stack) {
            return range;
        }

        @Override
        public boolean setRange(ItemStack stack, double range) {
            this.range = range;
            return true;
        }

        @Override
        public double getEnergyCost(ItemStack stack) {
            return energyCost;
        }

        @Override
        public boolean setEnergyCost(ItemStack stack, double energyCost) {
            this.energyCost = energyCost;
            return true;
        }

    }

    static class ComputingCore extends Item implements ComputingCoreComponent {

        int interval;
        int cost;

        double energyCost;

        public ComputingCore(Properties properties) {
            super(properties);
        }

        @Override
        public int getInterval(ItemStack stack) {
            return interval;
        }

        @Override
        public boolean setInterval(ItemStack stack, int interval) {
            this.interval = interval;
            return true;
        }

        @Override
        public int getCost(ItemStack stack) {
            return cost;
        }

        @Override
        public double getEnergyCost(ItemStack stack) {
            return energyCost;
        }

        @Override
        public boolean setEnergyCost(ItemStack stack, double energyCost) {
            this.energyCost = energyCost;
            return true;
        }

    }

    static class CommandShellContainer implements ShellContainer {

        List<ItemStack> itemStack;

        StateMachine stateMachine;

        Entity owner;

        boolean initialized = true;
        boolean enabled = true;
        boolean changed = false;

        public CommandShellContainer(StateMachine stateMachine, Entity owner, ItemStack... component) {
            this.itemStack = new ArrayList<>();
            Collections.addAll(this.itemStack, component);
            this.stateMachine = stateMachine;
            this.owner = owner;
        }

        @Override
        public ItemStack getInventorySlot(int slot) {
            return itemStack.get(slot);
        }

        @Override
        public void setInventorySlot(int slot, ItemStack stack) {
            itemStack.set(slot, stack);
        }

        @Override
        public StateMachine getStateMachine() {
            return stateMachine;
        }

        @Override
        public Entity getOwner() {
            return owner;
        }

        @Override
        public void setOwner(Entity owner) {
            this.owner = owner;
        }

        @Override
        public int getCoreCost() {
            if (itemStack.get(0).getItem() instanceof ComputingCore core) {
                return core.getCost(itemStack.get(0));
            }
            return 0;
        }

        @Override
        public int getInterval() {
            if (itemStack.get(0).getItem() instanceof ComputingCore core) {
                return core.getInterval(itemStack.get(0));
            }
            return 0;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void setInitialized(boolean initialized) {
            this.initialized = initialized;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public double getEnergy() {
            if (itemStack.get(1).getItem() instanceof EnergyModule energyModule) {
                return energyModule.getStoredEnergy(itemStack.get(1));
            }
            return 0;
        }

        @Override
        public void setEnergy(double energy) {
            if (itemStack.get(1).getItem() instanceof EnergyModule energyModule) {
                energyModule.addEnergy(itemStack.get(1), energy);
            }
        }

        @Override
        public void setChanged() {
            this.changed = !this.changed;
        }

        @Override
        public boolean isClientSide() {
            return changed;
        }

        @Override
        public ItemStack getCoreStack() {
            return getInventorySlot(0);
        }

        @Override
        public ItemStack getDiskStack() {
            return getInventorySlot(3);
        }

        @Override
        public ItemStack getEnergyStack() {
            return getInventorySlot(1);
        }

        @Override
        public ItemStack getInteractorStack() {
            return getInventorySlot(2);
        }

        @Override
        public boolean hasOwner() {
            if (this.owner != null && owner instanceof Player) {
                return true;
            }
            return false;
        }

        @Override
        public boolean hasWorldInteractor() {
            if (this.getInteractorStack().getItem() instanceof WorldInteractorComponent) {
                return true;
            }
            return false;
        }
    }

}
