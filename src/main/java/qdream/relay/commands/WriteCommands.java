package qdream.relay.commands;

import com.mojang.brigadier.CommandDispatcher;
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
import qdream.relay.items.SpellDiskItem;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;

import java.util.List;

/**
 * 写入法术命令
 */
public class WriteCommands {

    private static final SimpleCommandExceptionType NO_DISK = new SimpleCommandExceptionType(
            Component.literal("手中没有法术磁盘")
    );

    private static final SimpleCommandExceptionType INVALID_SLOT = new SimpleCommandExceptionType(
            Component.literal("无效的插槽位置")
    );

    /**
     * 注册写入命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, LiteralCommandNode<CommandSourceStack> root) {
        // /relay write <hand|shell> [slot] <program_string>
        LiteralCommandNode<CommandSourceStack> write = Commands.literal("write")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("program", StringArgumentType.greedyString())
                                .executes(WriteCommands::writeSpellToHand)
                        )
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .then(Commands.argument("program", StringArgumentType.greedyString())
                                                .executes(WriteCommands::writeSpellToShell)
                                        )
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
                                .executes(WriteCommands::jsonWriteToHand)
                        )
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .then(Commands.argument("json", StringArgumentType.greedyString())
                                                .executes(WriteCommands::jsonWriteToShell)
                                        )
                                )
                        )
                )
                .build();

        root.addChild(write);
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

        var pos = CommandUtils.parseBlockPos(posStr, source);
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof ShellContainer shell)) {
            throw INVALID_SLOT.create();
        }

        ItemStack disk = shell.getDiskStack();
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
            source.sendFailure(Component.literal("§c JSON 解析失败：" + e.getMessage()));
            return 0;
        }

        List<Executable> program = SpellDiskItem.getProgram(stack);
        source.sendSuccess(() -> Component.literal("已写入 JSON 程序：§e" + program.size() + "§r 个指令"), true);
        return program.size();
    }

    /**
     * JSON 写入外壳方块中的法术磁盘
     */
    private static int jsonWriteToShell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");
        String jsonStr = StringArgumentType.getString(context, "json");

        var pos = CommandUtils.parseBlockPos(posStr, source);
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
            source.sendFailure(Component.literal("§c JSON 解析失败：" + e.getMessage()));
            return 0;
        }
        shell.setChanged();

        List<Executable> program = SpellDiskItem.getProgram(disk);
        source.sendSuccess(() -> Component.literal("已向外壳写入 JSON 程序：§e" + program.size() + "§r 个指令"), true);
        return program.size();
    }
}
