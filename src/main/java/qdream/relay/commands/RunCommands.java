package qdream.relay.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.SpellDiskItem;
import qdream.relay.mc.component.EnergyModuleComponent;
import qdream.relay.mc.component.WorldInteractorComponent;
import qdream.relay.mc.base.Operation;
import qdream.relay.mc.base.Spell;
import qdream.relay.mc.component.ComputingCoreComponent;

/**
 * 运行法术命令
 *
 * 命令格式：
 * /relay run hand [ops] [withInteractor] [energy] [owner] [self]
 */
public class RunCommands {
    /**
     * 注册运行命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            LiteralCommandNode<CommandSourceStack> root) {
        // /relay run hand
        var handNode = Commands.literal("hand");

        // // 构建参数链：self <- owner <- energy <- worldInteractor <- ops
        // var selfNode = Commands.argument("self", StringArgumentType.word())
        // .executes(RunCommands::runHandFull);

        // var ownerNode = Commands.argument("owner", StringArgumentType.word())
        // .then(selfNode);

        // var energyNode = Commands.argument("energy", DoubleArgumentType.doubleArg(0))
        // .then(ownerNode);

        // var interactorNode = Commands.argument("worldInteractor",
        // BoolArgumentType.bool())
        // .then(energyNode);

        // var opsNode = Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
        // .executes(RunCommands::runHandWithOps)
        // .then(interactorNode);

        // handNode.executes(RunCommands::runHand)
        // .then(opsNode);

        var runHand = handNode.build();

        // /relay run
        var runNode = Commands.literal("run")
                .then(runHand)
                .build();

        root.addChild(runNode);
    }

    // 错误定义
    private static final SimpleCommandExceptionType NO_DISK = new SimpleCommandExceptionType(
            Component.literal("手中没有法术磁盘"));

    /**
     * 模拟执行指定数量的操作
     *
     * @param machine 状态机
     * @param maxCost 最大执行操作数
     * @return 实际消耗的操作数
     */
    private static int executeOps(StateMachine machine, int maxCost, ItemStack energyModule) {
        int executedCost = 0;

        var program = machine.peekProgram();
        if (program instanceof Operation operation) {
            executedCost += operation.getCost();
        }
        if (program instanceof Spell spell) {
            ((EnergyModule) energyModule.getItem()).consumeEnergy(energyModule, spell.getEnergy());
        }

        while (executedCost < maxCost && machine.isRunning()) {
            if (!machine.step()) {
                break;
            }
        }

        return executedCost;
    }

    /**
     * 运行手中的法术磁盘（基础版）
     */
    private static int runHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player player = source.getPlayerOrException();
        ItemStack diskStack = player.getMainHandItem();

        if (!(diskStack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        // 在方法内部创建临时物品
        ItemStack energyModule = new ItemStack(new EnergyModule(new Item.Properties()));
        ItemStack worldInteractor = new ItemStack(new WorldInteractor(new Item.Properties()));
        ItemStack computingCore = new ItemStack(new ComputingCore(new Item.Properties()));

        // 根据命令设置物品

        // 创建运行栈机
        StateMachine stateMachine = new StateMachine();

        // 设置Shell容器
        CommandShellContainer commandShellContainer = new CommandShellContainer(stateMachine, player, computingCore,
                energyModule, worldInteractor, diskStack);
        commandShellContainer.setOwner(player);

        // 设置栈机器运行上下文
        stateMachine.setContext("self", player);
        stateMachine.setContext("level", source.getLevel());
        stateMachine.setContext("shellContainer", commandShellContainer);

        // 开始运行
        return executeOps(stateMachine, 0, energyModule);

    }

    // 定义各各组件
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
            Collections.addAll(itemStack, component);
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
