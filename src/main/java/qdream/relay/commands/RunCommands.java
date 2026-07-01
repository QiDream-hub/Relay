package qdream.relay.commands;

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
import net.minecraft.commands.arguments.item.ItemArgument;
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
 * 支持设置完整的运行上下文：主人、自身、世界交互器、能量模块、能量值
 */
public class RunCommands {

    private static final SimpleCommandExceptionType NO_DISK = new SimpleCommandExceptionType(
            Component.literal("手中没有法术磁盘"));

    private static final SimpleCommandExceptionType INVALID_SLOT = new SimpleCommandExceptionType(
            Component.literal("无效的插槽位置"));

    private static final SimpleCommandExceptionType NO_PLAYER = new SimpleCommandExceptionType(
            Component.literal("无法获取玩家实体"));

    /**
     * 模拟执行指定数量的操作
     * 命令调试场景使用，不考虑能量消耗
     *
     * @param machine 状态机
     * @param maxOps  最大执行操作数
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
     * 为状态机设置完整的运行上下文
     *
     * @param machine 状态机
     * @param container 外壳容器
     * @param owner 所有者（主人）
     * @param self 自身引用（通常为执行命令的玩家）
     * @param energy 能量值（-1 表示无限）
     */
    private static void setupContext(StateMachine machine, ShellContainer container,
                                     Entity owner, Entity self, double energy) {
        // 设置外壳容器上下文（支持 get_self、get_owner 等操作）
        machine.setContext("shellContainer", container);

        // 设置主人（owner）- 法术的所有者
        if (owner != null) {
            machine.setContext("owner", owner);
        }

        // 设置自身引用（self）- 执行命令的玩家
        if (self != null) {
            machine.setContext("self", self);
        }

        // 设置能量值（负数表示无限能量）
        if (energy >= 0) {
            machine.setContext("energy", energy);
        }
    }

    /**
     * 创建手持容器的伪实现
     * 用于命令执行时提供 shellContainer 上下文
     *
     * @param diskStack 法术磁盘物品
     * @param player 玩家（主人）
     * @param interactorStack 世界交互器物品（可为空）
     * @param energyModule 能量模块物品（可为空，表示无限能量）
     * @param energy 能量值（-1 表示使用能量模块或无限）
     * @return 伪容器
     */
    private static ShellContainer createHandContainer(ItemStack diskStack, Player player,
                                                       ItemStack interactorStack,
                                                       ItemStack energyModule, double energy) {
        var shell = new ShellContainer() {
            Entity owner;

            @Override
            public ItemStack getInventorySlot(int slot) {
                return switch (slot) {
                    case DISK_SLOT -> diskStack;
                    case INTERACTOR_SLOT -> interactorStack != null ? interactorStack : ItemStack.EMPTY;
                    case ENERGY_SLOT -> energyModule != null ? energyModule : ItemStack.EMPTY;
                    default -> ItemStack.EMPTY;
                };
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
                return 9999;
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
                return energy >= 0 ? energy : 999999999;
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
     *
     * 命令格式：
     * /relay run hand <ops> [withWorldInteractor] [energyModule] [energy] [owner] [self]
     * /relay run shell <pos> <ops> [withWorldInteractor] [energy] [owner] [self]
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            LiteralCommandNode<CommandSourceStack> root) {
        // /relay run hand <ops>
        LiteralCommandNode<CommandSourceStack> runHand = Commands.literal("hand")
                .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                        .executes(RunCommands::runHand)
                        // [withWorldInteractor]
                        .then(Commands.argument("withWorldInteractor", BoolArgumentType.bool())
                                .executes(RunCommands::runHandWithInteractor)
                                // [energy]
                                .then(Commands.argument("energy", DoubleArgumentType.doubleArg(0))
                                        .executes(RunCommands::runHandWithInteractorAndEnergy)
                                        // [owner]
                                        .then(Commands.argument("owner", StringArgumentType.word())
                                                .executes(RunCommands::runHandFull)
                                                // [self]
                                                .then(Commands.argument("self", StringArgumentType.word())
                                                        .executes(RunCommands::runHandFull))))))
                .build();

        // /relay run shell <pos> <ops>
        LiteralCommandNode<CommandSourceStack> runShell = Commands.literal("shell")
                .then(Commands.argument("pos", StringArgumentType.word())
                        .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                                .executes(RunCommands::runShell)
                                // [withWorldInteractor]
                                .then(Commands.argument("withWorldInteractor", BoolArgumentType.bool())
                                        .executes(RunCommands::runShellWithInteractor)
                                        // [energy]
                                        .then(Commands.argument("energy", DoubleArgumentType.doubleArg(0))
                                                .executes(RunCommands::runShellWithInteractorAndEnergy)
                                                // [owner]
                                                .then(Commands.argument("owner", StringArgumentType.word())
                                                        .executes(RunCommands::runShellFull)
                                                        // [self]
                                                        .then(Commands.argument("self", StringArgumentType.word())
                                                                .executes(RunCommands::runShellFull)))))))
                .build();

        LiteralCommandNode<CommandSourceStack> run = Commands.literal("run")
                .then(runHand)
                .then(runShell)
                .build();

        root.addChild(run);
    }

    /**
     * 运行手中的法术磁盘程序（完整参数版）
     * /relay run hand <ops> [withWorldInteractor] [energy] [owner] [self]
     */
    private static int runHandFull(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack diskStack = player.getMainHandItem();

        if (!(diskStack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        int ops = IntegerArgumentType.getInteger(context, "ops");
        boolean useWorldInteractor = context.getArgument("withWorldInteractor", Boolean.class);
        double energy = context.getArgument("energy", Double.class);
        String ownerName = context.getArgument("owner", String.class);
        String selfName = context.getArgument("self", String.class);

        List<Executable> program = SpellDiskItem.getProgram(diskStack);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空，无法运行"), true);
            return 0;
        }

        // 解析所有者和自身实体
        Entity owner = parseEntity(source, ownerName, player);
        Entity self = parseEntity(source, selfName, player);

        // 创建世界交互器（如果需要）
        ItemStack interactorStack = useWorldInteractor ? createDebugInteractor() : ItemStack.EMPTY;

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        // 创建容器并设置上下文
        ShellContainer container = createHandContainer(diskStack, player, interactorStack, null, energy);
        setupContext(machine, container, owner, self, energy);

        // 如果启用世界交互器，设置世界交互器上下文
        if (useWorldInteractor) {
            machine.setContext("worldInteractor", interactorStack);
        }

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
     * 运行手中的法术磁盘程序（带能量参数）
     */
    private static int runHandWithInteractorAndEnergy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack diskStack = player.getMainHandItem();

        if (!(diskStack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        int ops = IntegerArgumentType.getInteger(context, "ops");
        boolean useWorldInteractor = context.getArgument("withWorldInteractor", Boolean.class);
        double energy = context.getArgument("energy", Double.class);

        List<Executable> program = SpellDiskItem.getProgram(diskStack);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空，无法运行"), true);
            return 0;
        }

        // 创建世界交互器（如果需要）
        ItemStack interactorStack = useWorldInteractor ? createDebugInteractor() : ItemStack.EMPTY;

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        // 创建容器并设置上下文（主人和自身都设为玩家）
        ShellContainer container = createHandContainer(diskStack, player, interactorStack, null, energy);
        setupContext(machine, container, player, player, energy);

        // 如果启用世界交互器，设置世界交互器上下文
        if (useWorldInteractor) {
            machine.setContext("worldInteractor", interactorStack);
        }

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
     * 运行手中的法术磁盘程序（带世界交互器选项）
     */
    private static int runHandWithInteractor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack diskStack = player.getMainHandItem();

        if (!(diskStack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        int ops = IntegerArgumentType.getInteger(context, "ops");
        boolean useWorldInteractor = context.getArgument("withWorldInteractor", Boolean.class);

        List<Executable> program = SpellDiskItem.getProgram(diskStack);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空，无法运行"), true);
            return 0;
        }

        // 创建世界交互器（如果需要）
        ItemStack interactorStack = useWorldInteractor ? createDebugInteractor() : ItemStack.EMPTY;

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        // 创建容器并设置上下文（主人和自身都设为玩家，无限能量）
        ShellContainer container = createHandContainer(diskStack, player, interactorStack, null, -1);
        setupContext(machine, container, player, player, -1);

        // 如果启用世界交互器，设置世界交互器上下文
        if (useWorldInteractor) {
            machine.setContext("worldInteractor", interactorStack);
        }

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
     * 运行手中的法术磁盘程序（基础版）
     */
    private static int runHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack diskStack = player.getMainHandItem();

        if (!(diskStack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        int ops = IntegerArgumentType.getInteger(context, "ops");

        List<Executable> program = SpellDiskItem.getProgram(diskStack);
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

        // 创建容器并设置上下文（主人和自身都设为玩家，无限能量）
        ShellContainer container = createHandContainer(diskStack, player, null, null, -1);
        setupContext(machine, container, player, player, -1);

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
     * 运行外壳方块中的法术磁盘程序（完整参数版）
     * /relay run shell <pos> <ops> [withWorldInteractor] [energy] [owner] [self]
     */
    private static int runShellFull(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");
        int ops = IntegerArgumentType.getInteger(context, "ops");
        boolean useWorldInteractor = context.getArgument("withWorldInteractor", Boolean.class);
        double energy = context.getArgument("energy", Double.class);
        String ownerName = context.getArgument("owner", String.class);
        String selfName = context.getArgument("self", String.class);

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

        // 解析所有者和自身实体
        Entity owner = parseEntity(source, ownerName, source.getPlayerOrException());
        Entity self = parseEntity(source, selfName, source.getPlayerOrException());

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        // 设置上下文
        setupContext(machine, shell, owner, self, energy);

        // 如果启用世界交互器，设置世界交互器上下文
        if (useWorldInteractor) {
            machine.setContext("worldInteractor", shell.getInteractorStack());
        }

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
     * 运行外壳方块中的法术磁盘程序（带能量参数）
     */
    private static int runShellWithInteractorAndEnergy(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");
        int ops = IntegerArgumentType.getInteger(context, "ops");
        boolean useWorldInteractor = context.getArgument("withWorldInteractor", Boolean.class);
        double energy = context.getArgument("energy", Double.class);

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

        // 获取玩家作为主人和自身
        var player = source.getPlayerOrException();

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        // 设置上下文（主人和自身都设为玩家）
        setupContext(machine, shell, player, player, energy);

        // 如果启用世界交互器，设置世界交互器上下文
        if (useWorldInteractor) {
            machine.setContext("worldInteractor", shell.getInteractorStack());
        }

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
        boolean useWorldInteractor = context.getArgument("withWorldInteractor", Boolean.class);

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

        // 获取玩家作为主人和自身
        var player = source.getPlayerOrException();

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        // 设置上下文（主人和自身都设为玩家，无限能量）
        setupContext(machine, shell, player, player, -1);

        // 如果启用世界交互器，设置世界交互器上下文
        if (useWorldInteractor) {
            machine.setContext("worldInteractor", shell.getInteractorStack());
        }

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

        // 获取玩家作为主人和自身
        var player = source.getPlayerOrException();

        StateMachine machine = new StateMachine();
        machine.setMishapHandler(reason -> {
            try {
                source.sendFailure(Component.literal("§c 事故：" + reason));
            } catch (Exception e) {
                // 忽略
            }
        });

        // 设置上下文（主人和自身都设为玩家，无限能量）
        setupContext(machine, shell, player, player, -1);

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
     * 解析实体名称
     *
     * @param source 命令来源
     * @param name 实体名称（"self" 表示执行命令的玩家，"owner" 表示方块所有者，或其他玩家名）
     * @param defaultEntity 默认实体（当名称无法解析时返回）
     * @return 解析的实体，失败返回默认实体
     */
    private static Entity parseEntity(CommandSourceStack source, String name, Entity defaultEntity) {
        if (name == null || name.equals("self")) {
            return source.getEntity() != null ? source.getEntity() : defaultEntity;
        }
        if (name.equals("owner")) {
            return defaultEntity;
        }
        // 尝试通过玩家名获取实体
        try {
            var server = source.getServer();
            var player = server.getPlayerList().getPlayerByName(name);
            if (player != null) {
                return player;
            }
        } catch (Exception e) {
            // 忽略
        }
        return defaultEntity;
    }

    /**
     * 创建调试用的世界交互器物品
     */
    private static ItemStack createDebugInteractor() {
        // 创建一个伪世界交互器（用于命令调试）
        // 实际使用时应该从物品注册表中获取真正的世界交互器
        return ItemStack.EMPTY; // 当前简化实现，返回空物品，操作通过检查 worldInteractor 上下文判断
    }
}
