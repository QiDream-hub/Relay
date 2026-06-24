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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
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

        // /relay read <hand|shell> [slot] [format]
        LiteralCommandNode<CommandSourceStack> read = Commands.literal("read")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .executes(RelayCommands::readHand)
                        .then(Commands.argument("format", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("json");
                                    builder.suggest("nbt");
                                    return builder.buildFuture();
                                })
                                .executes(RelayCommands::readHandFormatted)
                        )
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .executes(RelayCommands::readShell)
                                        .then(Commands.argument("format", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("json");
                                                    builder.suggest("nbt");
                                                    return builder.buildFuture();
                                                })
                                                .executes(RelayCommands::readShellFormatted)
                                        )
                                )
                        )
                )
                .build();

        // /relay run <hand|shell> [ops] [withWorldInteractor]
        LiteralCommandNode<CommandSourceStack> run = Commands.literal("run")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                                .executes(RelayCommands::runHand)
                                .then(Commands.argument("withWorldInteractor", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("true");
                                            builder.suggest("false");
                                            return builder.buildFuture();
                                        })
                                        .executes(RelayCommands::runHandWithInteractor)
                                )
                        )
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .then(Commands.argument("ops", IntegerArgumentType.integer(1, 10000))
                                                .executes(RelayCommands::runShell)
                                                .then(Commands.argument("withWorldInteractor", StringArgumentType.word())
                                                        .suggests((ctx, builder) -> {
                                                            builder.suggest("true");
                                                            builder.suggest("false");
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(RelayCommands::runShellWithInteractor)
                                                )
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

        source.sendSuccess(() -> copyableText("法术程序 (§e" + program.size() + "§r 个指令): §f", programToString(program)), true);
        return program.size();
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
     * 运行外壳方块中的法术磁盘程序（带世界交互器选项）
     */
    private static int runShellWithInteractor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");
        int ops = IntegerArgumentType.getInteger(context, "ops");
        String withInteractor = StringArgumentType.getString(context, "withWorldInteractor");
        boolean useWorldInteractor = Boolean.parseBoolean(withInteractor);

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

        // 如果启用世界交互器，设置上下文
        if (useWorldInteractor) {
            machine.setContext("worldInteractor", shell.getInteractorStack());
        }

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
     * 按格式读取手中的法术磁盘
     */
    private static int readHandFormatted(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof SpellDiskItem)) {
            throw NO_DISK.create();
        }

        String format = StringArgumentType.getString(context, "format");
        List<Executable> program = SpellDiskItem.getProgram(stack);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空"), true);
            return 0;
        }

        sendFormattedProgram(source, program, format);
        return program.size();
    }

    /**
     * 按格式读取外壳方块中的法术磁盘
     */
    private static int readShellFormatted(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");
        String format = StringArgumentType.getString(context, "format");

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

        sendFormattedProgram(source, program, format);
        return program.size();
    }

    /**
     * 按指定格式发送程序输出
     */
    private static void sendFormattedProgram(CommandSourceStack source, List<Executable> program, String format) {
        switch (format.toLowerCase()) {
            case "json" -> {
                String json = ProgramCompiler.toJsonString(program);
                source.sendSuccess(() -> copyableText("JSON 程序 (§e" + program.size() + "§r 个指令): §f", json), true);
            }
            case "nbt" -> {
                try {
                    String nbt = ProgramCompiler.toNbt(program).toString();
                    source.sendSuccess(() -> copyableText("NBT 程序 (§e" + program.size() + "§r 个指令): §f", nbt), true);
                } catch (ProgramCompiler.CompilationException e) {
                    source.sendFailure(Component.literal("§c NBT 序列化失败: " + e.getMessage()));
                }
            }
            default -> {
                source.sendSuccess(() -> copyableText("法术程序 (§e" + program.size() + "§r 个指令): §f", programToString(program)), true);
            }
        }
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

        source.sendSuccess(() -> copyableText("法术程序 (§e" + program.size() + "§r 个指令): §f", programToString(program)), true);
        return program.size();
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
     * 构建带点击复制功能的文本组件
     * 点击后内容会被填入聊天输入框，方便玩家复制
     */
    private static MutableComponent copyableText(String prefix, String content) {
        MutableComponent label = Component.literal(prefix);
        MutableComponent body = Component.literal(content)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.CopyToClipboard(content))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("§7点击复制到剪贴板")))
                        .withUnderlined(true)
                );
        return label.append(body);
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
