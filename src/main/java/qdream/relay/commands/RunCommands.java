package qdream.relay.commands;

import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.items.SpellDiskItem;
import qdream.relay.mc.base.Operation;

import java.util.List;

/**
 * 运行法术命令
 */
public class RunCommands {

    private static final SimpleCommandExceptionType NO_DISK = new SimpleCommandExceptionType(
            Component.literal("手中没有法术磁盘"));

    private static final SimpleCommandExceptionType INVALID_SLOT = new SimpleCommandExceptionType(
            Component.literal("无效的插槽位置"));

    /**
     * 模拟执行指定数量的操作
     * 命令调试场景使用，不考虑能量消耗
     *
     * @param machine 状态机
     * @param maxOps 最大执行操作数
     * @return 实际执行的操作数
     */
    private static int executeOps(StateMachine machine, int maxOps) {
        int executedCount = 0;
        int usedCost = 0;

        while (usedCost < maxOps && machine.isRunning()) {
            Executable top = machine.peekProgram();
            if (top == null) {
                break;
            }

            int cost = 1;
            if (top instanceof Operation op) {
                cost = op.getCost();
            }

            if (usedCost + cost > maxOps) {
                break;
            }

            if (!machine.step()) {
                break;
            }

            executedCount++;
            usedCost += cost;
        }

        return executedCount;
    }

    /**
     * 创建手持容器的伪实现
     * 用于命令执行时提供 shellContainer 上下文
     */
    private static ShellContainer createHandContainer(ItemStack stack, Player player) {
        var shell = new ShellContainer() {
            Entity owner;

            @Override
            public ItemStack getInventorySlot(int slot) {
                return slot == 1 ? stack : ItemStack.EMPTY;
            }

            @Override
            public void setInventorySlot(int slot, ItemStack itemStack) {
            }

            @Override
            public StateMachine getStateMachine() {
                return null;
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
                return true;
            }

            @Override
            public void setInitialized(boolean initialized) {
            }

            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public void setEnabled(boolean enabled) {
            }

            @Override
            public double getEnergy() {
                return 999999999;
            }

            @Override
            public void setEnergy(double energy) {
            }

            @Override
            public void setChanged() {
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
        };
        shell.setOwner(player);
        return shell;
    }

    /**
     * 注册运行命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            LiteralCommandNode<CommandSourceStack> root) {
        // /relay run <hand|shell> [ops] [withWorldInteractor]
        LiteralCommandNode<CommandSourceStack> run = Commands.literal("run")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                                .executes(RunCommands::runHand)
                                .then(Commands.argument("withWorldInteractor", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("true");
                                            builder.suggest("false");
                                            return builder.buildFuture();
                                        })
                                        .executes(RunCommands::runHandWithInteractor)))
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                                                .executes(RunCommands::runShell)
                                                .then(Commands
                                                        .argument("withWorldInteractor", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            builder.suggest("true");
                                                            builder.suggest("false");
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(RunCommands::runShellWithInteractor))))))
                .build();

        root.addChild(run);
    }

    /**
     * 运行手中的法术磁盘程序（带世界交互器选项）
     */
    private static int runHandWithInteractor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        int ops = IntegerArgumentType.getInteger(context, "ops");
        String withInteractor = StringArgumentType.getString(context, "withWorldInteractor");
        boolean useWorldInteractor = Boolean.parseBoolean(withInteractor);

        List<Executable> program = SpellDiskItem.getProgram(stack);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空，无法运行"), true);
            return 0;
        }

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        // 如果启用世界交互器，设置上下文
        if (useWorldInteractor) {
            machine.setContext("worldInteractor", stack);
        }

        // 设置 shellContainer 上下文，支持 get_self、get_owner 等操作
        machine.setContext("shellContainer", createHandContainer(stack, player));

        machine.loadProgram(program);
        int executedCount = executeOps(machine, ops);

        List<Executable> dataStack = machine.getDataStackSnapshot();
        StringBuilder result = new StringBuilder();
        result.append("运行完成 (执行 §e").append(executedCount).append("§r 个操作)");
        if (!dataStack.isEmpty()) {
            result.append(", 数据栈：§f").append(CommandUtils.dataStackToString(dataStack));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), true);
        return 1;
    }

    /**
     * 运行外壳方块中的法术磁盘程序（带世界交互器选项）
     */
    private static int runShellWithInteractor(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");
        int ops = IntegerArgumentType.getInteger(context, "ops");
        String withInteractor = StringArgumentType.getString(context, "withWorldInteractor");
        boolean useWorldInteractor = Boolean.parseBoolean(withInteractor);

        var pos = CommandUtils.parseBlockPos(posStr, source);
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof ShellContainer shell)) {
            throw INVALID_SLOT.create();
        }

        ItemStack disk = shell.getDiskStack();
        if (disk.isEmpty() || !(disk.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        List<Executable> program = SpellDiskItem.getProgram(disk);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空，无法运行"), true);
            return 0;
        }

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        // 如果启用世界交互器，设置上下文
        if (useWorldInteractor) {
            machine.setContext("worldInteractor", shell.getInteractorStack());
        }

        // 设置 shellContainer 上下文，支持 get_self、get_owner 等操作
        machine.setContext("shellContainer", shell);

        machine.loadProgram(program);
        int executedCount = executeOps(machine, ops);

        List<Executable> dataStack = machine.getDataStackSnapshot();
        StringBuilder result = new StringBuilder();
        result.append("运行完成 (执行 §e").append(executedCount).append("§r 个操作)");
        if (!dataStack.isEmpty()) {
            result.append(", 数据栈：§f").append(CommandUtils.dataStackToString(dataStack));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), true);
        return 1;
    }

    /**
     * 运行手中的法术磁盘程序
     */
    private static int runHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        int ops = IntegerArgumentType.getInteger(context, "ops");

        List<Executable> program = SpellDiskItem.getProgram(stack);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空，无法运行"), true);
            return 0;
        }

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        machine.loadProgram(program);
        int executedCount = executeOps(machine, ops);

        List<Executable> dataStack = machine.getDataStackSnapshot();
        StringBuilder result = new StringBuilder();
        result.append("运行完成 (执行 §e").append(executedCount).append("§r 个操作)");
        if (!dataStack.isEmpty()) {
            result.append(", 数据栈：§f").append(CommandUtils.dataStackToString(dataStack));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), true);
        return 1;
    }

    /**
     * 运行外壳方块中的法术磁盘程序
     */
    private static int runShell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");
        int ops = IntegerArgumentType.getInteger(context, "ops");

        var pos = CommandUtils.parseBlockPos(posStr, source);
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof ShellContainer shell)) {
            throw INVALID_SLOT.create();
        }

        ItemStack disk = shell.getDiskStack();
        if (disk.isEmpty() || !(disk.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        List<Executable> program = SpellDiskItem.getProgram(disk);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空，无法运行"), true);
            return 0;
        }

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        machine.loadProgram(program);
        int executedCount = executeOps(machine, ops);

        List<Executable> dataStack = machine.getDataStackSnapshot();
        StringBuilder result = new StringBuilder();
        result.append("运行完成 (执行 §e").append(executedCount).append("§r 个操作)");
        if (!dataStack.isEmpty()) {
            result.append(", 数据栈：§f").append(CommandUtils.dataStackToString(dataStack));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), true);
        return 1;
    }
}
