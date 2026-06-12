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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import qdream.relay.core.ShellContainer;
import qdream.relay.engine.Executable;
import qdream.relay.engine.StateMachine;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;
import qdream.relay.types.NumberIota;
import qdream.relay.types.StringIota;
import qdream.relay.types.BooleanIota;
import qdream.relay.types.ListIota;
import qdream.relay.types.VectorIota;
import qdream.relay.mc.base.Operation;
import qdream.relay.items.SpellDiskItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Relay 模组命令注册
 */
public class RelayCommands {

    private static final SimpleCommandExceptionType NO_DISK = new SimpleCommandExceptionType(
            Component.literal("手中没有法术磁盘")
    );

    private static final SimpleCommandExceptionType INVALID_SLOT = new SimpleCommandExceptionType(
            Component.literal("无效的插槽位置")
    );

    private static final SimpleCommandExceptionType RUN_ERROR = new SimpleCommandExceptionType(
            Component.literal("运行程序时出错")
    );

    /**
     * 注册所有命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("relay")
                .build();

        // /relay write <hand|shell> [slot] <program_string>
        LiteralCommandNode<CommandSourceStack> write = Commands.literal("write")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("program", StringArgumentType.greedyString())
                                .executes(RelayCommands::writeSpellToHand)
                        )
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .then(Commands.argument("program", StringArgumentType.greedyString())
                                                .executes(RelayCommands::writeSpellToShell)
                                        )
                                )
                        )
                )
                .build();

        // /relay clear <hand|shell> [slot]
        LiteralCommandNode<CommandSourceStack> clear = Commands.literal("clear")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .executes(RelayCommands::clearHand)
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .executes(RelayCommands::clearShell)
                                )
                        )
                )
                .build();

        // /relay read <hand|shell> [slot]
        LiteralCommandNode<CommandSourceStack> read = Commands.literal("read")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .executes(RelayCommands::readHand)
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .executes(RelayCommands::readShell)
                                )
                        )
                )
                .build();

        // /relay run <hand|shell> [slot] [ops]
        LiteralCommandNode<CommandSourceStack> run = Commands.literal("run")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                                .executes(RelayCommands::runHand)
                        )
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                                                .executes(RelayCommands::runShell)
                                        )
                                )
                        )
                )
                .build();

        dispatcher.getRoot().addChild(root);
        root.addChild(write);
        root.addChild(clear);
        root.addChild(read);
        root.addChild(run);

        // /relay jsonread <hand|shell> [slot]
        LiteralCommandNode<CommandSourceStack> jsonread = Commands.literal("jsonread")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .executes(RelayCommands::jsonReadHand)
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .executes(RelayCommands::jsonReadShell)
                                )
                        )
                )
                .build();

        // /relay jsonwrite <hand|shell> [slot] <json>
        LiteralCommandNode<CommandSourceStack> jsonwrite = Commands.literal("jsonwrite")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("json", StringArgumentType.greedyString())
                                .executes(RelayCommands::jsonWriteToHand)
                        )
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .then(Commands.argument("json", StringArgumentType.greedyString())
                                                .executes(RelayCommands::jsonWriteToShell)
                                        )
                                )
                        )
                )
                .build();

        root.addChild(jsonread);
        root.addChild(jsonwrite);
    }

    /**
     * 向手中的法术磁盘写入法术
     */
    private static int writeSpellToHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();

        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        String programStr = StringArgumentType.getString(context, "program");
        List<Executable> program;
        try {
            program = ProgramCompiler.compileFromJson(programStr);
        } catch (CompilationException e) {
            source.sendFailure(Component.literal("§c 程序编译失败：" + e.getMessage()));
            return 0;
        }

        SpellDiskItem.setProgram(stack, program);

        source.sendSuccess(() -> Component.literal("已写入法术程序：§e" + program.size() + "§r 个指令"), true);
        return program.size();
    }

    /**
     * 向外壳方块中的法术磁盘写入法术
     */
    private static int writeSpellToShell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");
        String programStr = StringArgumentType.getString(context, "program");

        var pos = parseBlockPos(posStr, source);
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof ShellContainer shell)) {
            throw INVALID_SLOT.create();
        }

        ItemStack disk = shell.getDiskStack(); // 插槽 1 是法术磁盘
        if (disk.isEmpty() || !(disk.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        List<Executable> program;
        try {
            program = ProgramCompiler.compileFromJson(programStr);
        } catch (CompilationException e) {
            source.sendFailure(Component.literal("§c 程序编译失败：" + e.getMessage()));
            return 0;
        }
        SpellDiskItem.setProgram(disk, program);
        shell.setChanged();

        source.sendSuccess(() -> Component.literal("已向外壳写入法术程序：§e" + program.size() + "§r 个指令"), true);
        return program.size();
    }

    /**
     * 清空手中的法术磁盘
     */
    private static int clearHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        SpellDiskItem.clear(stack);
        source.sendSuccess(() -> Component.literal("已清空法术磁盘"), true);
        return 1;
    }

    /**
     * 清空外壳方块中的法术磁盘
     */
    private static int clearShell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");

        var pos = parseBlockPos(posStr, source);
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof ShellContainer shell)) {
            throw INVALID_SLOT.create();
        }

        ItemStack disk = shell.getDiskStack();
        if (disk.isEmpty() || !(disk.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        SpellDiskItem.clear(disk);
        shell.setChanged();
        source.sendSuccess(() -> Component.literal("已清空外壳中的法术磁盘"), true);
        return 1;
    }

    /**
     * 读取手中的法术磁盘
     */
    private static int readHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        List<Executable> program = SpellDiskItem.getProgram(stack);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空"), true);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("法术程序 (§e" + program.size() + "§r 个指令): §f" + programToString(program)), true);
        return program.size();
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
        machine.run(ops);

        List<Executable> dataStack = machine.getDataStackSnapshot();
        StringBuilder result = new StringBuilder();
        result.append("运行完成 (剩余 §e").append(machine.getRemainingOps()).append("§r 操作)");
        if (!dataStack.isEmpty()) {
            result.append(", 数据栈：§f").append(dataStackToString(dataStack));
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

        var pos = parseBlockPos(posStr, source);
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
        machine.run(ops);

        List<Executable> dataStack = machine.getDataStackSnapshot();
        StringBuilder result = new StringBuilder();
        result.append("运行完成 (剩余 §e").append(machine.getRemainingOps()).append("§r 操作)");
        if (!dataStack.isEmpty()) {
            result.append(", 数据栈：§f").append(dataStackToString(dataStack));
        }

        source.sendSuccess(() -> Component.literal(result.toString()), true);
        return 1;
    }

    /**
     * 读取外壳方块中的法术磁盘
     */
    private static int readShell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");

        var pos = parseBlockPos(posStr, source);
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
            source.sendSuccess(() -> Component.literal("法术磁盘为空"), true);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("法术程序 (§e" + program.size() + "§r 个指令): §f" + programToString(program)), true);
        return program.size();
    }

    /**
     * JSON 读取手中的法术磁盘
     */
    private static int jsonReadHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        String json = SpellDiskItem.exportToJson(stack);
        source.sendSuccess(() -> Component.literal("JSON 程序: §f" + json), true);
        return 1;
    }

    /**
     * JSON 读取外壳方块中的法术磁盘
     */
    private static int jsonReadShell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");

        var pos = parseBlockPos(posStr, source);
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof ShellContainer shell)) {
            throw INVALID_SLOT.create();
        }

        ItemStack disk = shell.getDiskStack();
        if (disk.isEmpty() || !(disk.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        String json = SpellDiskItem.exportToJson(disk);
        source.sendSuccess(() -> Component.literal("JSON 程序: §f" + json), true);
        return 1;
    }

    /**
     * JSON 写入手中的法术磁盘
     */
    private static int jsonWriteToHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        String jsonStr = StringArgumentType.getString(context, "json");
        try {
            SpellDiskItem.importFromJson(stack, jsonStr);
        } catch (CompilationException e) {
            source.sendFailure(Component.literal("§c JSON 解析失败: " + e.getMessage()));
            return 0;
        }

        List<Executable> program = SpellDiskItem.getProgram(stack);
        source.sendSuccess(() -> Component.literal("已写入 JSON 程序: §e" + program.size() + "§r 个指令"), true);
        return program.size();
    }

    /**
     * JSON 写入外壳方块中的法术磁盘
     */
    private static int jsonWriteToShell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");
        String jsonStr = StringArgumentType.getString(context, "json");

        var pos = parseBlockPos(posStr, source);
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof ShellContainer shell)) {
            throw INVALID_SLOT.create();
        }

        ItemStack disk = shell.getDiskStack();
        if (disk.isEmpty() || !(disk.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        try {
            SpellDiskItem.importFromJson(disk, jsonStr);
        } catch (CompilationException e) {
            source.sendFailure(Component.literal("§c JSON 解析失败: " + e.getMessage()));
            return 0;
        }
        shell.setChanged();

        List<Executable> program = SpellDiskItem.getProgram(disk);
        source.sendSuccess(() -> Component.literal("已向外壳写入 JSON 程序: §e" + program.size() + "§r 个指令"), true);
        return program.size();
    }

    /**
     * 将程序转换为可读字符串
     */
    private static String programToString(List<Executable> program) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < program.size(); i++) {
            Executable exec = program.get(i);
            if (i > 0) sb.append("; ");

            if (exec instanceof qdream.relay.mc.base.Operation op) {
                String str = op.getId();
                if (str.startsWith("relay:")) {
                    sb.append(str.substring(6));
                } else {
                    sb.append("\"").append(str).append("\"");
                }
            } else if (exec instanceof StringIota s) {
                sb.append("\"").append(s.asString()).append("\"");
            } else if (exec instanceof NumberIota n) {
                sb.append(n.getValue());
            } else if (exec instanceof BooleanIota b) {
                sb.append(b.asBoolean());
            } else if (exec instanceof ListIota list) {
                sb.append("[...]");
            } else {
                sb.append(((Operation)exec).getId());
            }
        }
        return sb.toString();
    }

    /**
     * 将数据栈转换为可读字符串
     */
    private static String dataStackToString(List<Executable> dataStack) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dataStack.size(); i++) {
            Executable data = dataStack.get(i);
            if (i > 0) sb.append(", ");

            if (data instanceof StringIota s) {
                sb.append("\"").append(s.asString()).append("\"");
            } else if (data instanceof NumberIota n) {
                sb.append(n.getValue());
            } else if (data instanceof BooleanIota b) {
                sb.append(b.asBoolean());
            } else {
                sb.append(((Operation)data).getId());
            }
        }
        return sb.toString();
    }

    /**
     * 解析方块坐标字符串
     */
    private static net.minecraft.core.BlockPos parseBlockPos(String posStr, CommandSourceStack source) throws CommandSyntaxException {
        String[] parts = posStr.split(",");
        if (parts.length != 3) {
            throw INVALID_SLOT.create();
        }
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new net.minecraft.core.BlockPos(x, y, z);
        } catch (NumberFormatException e) {
            throw INVALID_SLOT.create();
        }
    }
}
