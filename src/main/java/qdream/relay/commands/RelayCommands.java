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

import qdream.relay.blocks.entity.ShellBlockEntity;
import qdream.relay.core.ShellContainer;
import qdream.relay.engine.IExecutable;
import qdream.relay.items.SpellDiskItem;
import qdream.relay.mc.McIota;

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

    /**
     * 注册所有命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("relay")
                .build();

        // /relay write_spell <hand|shell> [slot] <program_string>
        LiteralCommandNode<CommandSourceStack> writeSpell = Commands.literal("write_spell")
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

        dispatcher.getRoot().addChild(root);
        root.addChild(writeSpell);
        root.addChild(clear);
        root.addChild(read);
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
        List<IExecutable> program = parseProgram(programStr);

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

        List<IExecutable> program = parseProgram(programStr);
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

        List<IExecutable> program = SpellDiskItem.getProgram(stack);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空"), true);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("法术程序 (§e" + program.size() + "§r 个指令): §f" + programToString(program)), true);
        return program.size();
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

        List<IExecutable> program = SpellDiskItem.getProgram(disk);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空"), true);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("法术程序 (§e" + program.size() + "§r 个指令): §f" + programToString(program)), true);
        return program.size();
    }

    /**
     * 解析法术程序字符串
     * 格式：指令 1;指令 2;指令 3
     * 支持：push <值>, pop, dup, swap, add, sub, mul, div, eval 等
     */
    private static List<IExecutable> parseProgram(String programStr) {
        List<IExecutable> program = new ArrayList<>();
        String[] instructions = programStr.split(";");

        for (String instr : instructions) {
            instr = instr.trim();
            if (instr.isEmpty()) continue;

            if (instr.equals("pop")) {
                program.add(createOperation("relay:pop"));
            } else if (instr.equals("dup")) {
                program.add(createOperation("relay:dup"));
            } else if (instr.equals("swap")) {
                program.add(createOperation("relay:swap"));
            } else if (instr.equals("add")) {
                program.add(createOperation("relay:add"));
            } else if (instr.equals("sub")) {
                program.add(createOperation("relay:sub"));
            } else if (instr.equals("mul")) {
                program.add(createOperation("relay:mul"));
            } else if (instr.equals("div")) {
                program.add(createOperation("relay:div"));
            } else if (instr.equals("eval")) {
                program.add(createOperation("relay:eval"));
            } else if (instr.equals("if")) {
                program.add(createOperation("relay:if"));
            } else if (instr.equals("and")) {
                program.add(createOperation("relay:and"));
            } else if (instr.equals("or")) {
                program.add(createOperation("relay:or"));
            } else if (instr.equals("not")) {
                program.add(createOperation("relay:not"));
            } else if (instr.equals("eq")) {
                program.add(createOperation("relay:eq"));
            } else if (instr.equals("lt")) {
                program.add(createOperation("relay:lt"));
            } else if (instr.equals("gt")) {
                program.add(createOperation("relay:gt"));
            } else if (instr.equals("send")) {
                program.add(createOperation("relay:send"));
            } else if (instr.equals("recv")) {
                program.add(createOperation("relay:recv"));
            } else if (instr.equals("peek")) {
                program.add(createOperation("relay:peek"));
            } else {
                // 未知指令作为字符串压入
                program.add(McIota.ofString(instr));
            }
        }

        return program;
    }

    /**
     * 创建操作 iota
     */
    private static McIota createOperation(String opId) {
        return McIota.ofString(opId);
    }

    /**
     * 将程序转换为可读字符串
     */
    private static String programToString(List<IExecutable> program) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < program.size(); i++) {
            IExecutable exec = program.get(i);
            if (i > 0) sb.append("; ");

            if (exec instanceof McIota iota) {
                if (iota.isString()) {
                    String str = iota.asString();
                    if (str.startsWith("relay:")) {
                        sb.append(str.substring(6));
                    } else {
                        sb.append("\"").append(str).append("\"");
                    }
                } else if (iota.isNumber()) {
                    sb.append(iota.getValue());
                } else if (iota.isBoolean()) {
                    sb.append(iota.asBoolean());
                } else {
                    sb.append(iota.getType());
                }
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
