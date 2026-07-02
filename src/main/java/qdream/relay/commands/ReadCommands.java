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
import qdream.relay.mc.component.SpellDiskComponent;

import java.util.List;

/**
 * 读取法术命令
 */
public class ReadCommands {

    private static final SimpleCommandExceptionType NO_DISK = new SimpleCommandExceptionType(
            Component.literal("手中没有法术磁盘")
    );

    private static final SimpleCommandExceptionType INVALID_SLOT = new SimpleCommandExceptionType(
            Component.literal("无效的插槽位置")
    );

    /**
     * 注册读取命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, LiteralCommandNode<CommandSourceStack> root) {
        // /relay read <hand|shell> [slot] [format]
        LiteralCommandNode<CommandSourceStack> read = Commands.literal("read")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .executes(ReadCommands::readHand)
                        .then(Commands.argument("format", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("json");
                                    builder.suggest("nbt");
                                    return builder.buildFuture();
                                })
                                .executes(ReadCommands::readHandFormatted)
                        )
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .executes(ReadCommands::readShell)
                                        .then(Commands.argument("format", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("json");
                                                    builder.suggest("nbt");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ReadCommands::readShellFormatted)
                                        )
                                )
                        )
                )
                .build();

        root.addChild(read);
    }

    /**
     * 读取手中的法术磁盘
     */
    private static int readHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        SpellDiskComponent diskComponent = getDiskComponent(stack);
        if (diskComponent == null) {
            throw NO_DISK.create();
        }

        List<Executable> program = diskComponent.getProgram(stack);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空"), true);
            return 0;
        }

        source.sendSuccess(() -> CommandUtils.copyableText("法术程序 (§e" + program.size() + "§r 个指令): §f", CommandUtils.programToString(program)), true);
        return program.size();
    }

    /**
     * 读取外壳方块中的法术磁盘
     */
    private static int readShell(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String posStr = StringArgumentType.getString(context, "pos");

        var pos = CommandUtils.parseBlockPos(posStr, source);
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof ShellContainer shell)) {
            throw INVALID_SLOT.create();
        }

        ItemStack disk = shell.getDiskStack();
        if (disk.isEmpty()) {
            throw NO_DISK.create();
        }
        SpellDiskComponent diskComponent = getDiskComponent(disk);
        if (diskComponent == null) {
            throw NO_DISK.create();
        }

        List<Executable> program = diskComponent.getProgram(disk);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空"), true);
            return 0;
        }

        source.sendSuccess(() -> CommandUtils.copyableText("法术程序 (§e" + program.size() + "§r 个指令): §f", CommandUtils.programToString(program)), true);
        return program.size();
    }

    /**
     * 按格式读取手中的法术磁盘
     */
    private static int readHandFormatted(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        SpellDiskComponent diskComponent = getDiskComponent(stack);
        if (diskComponent == null) {
            throw NO_DISK.create();
        }

        String format = StringArgumentType.getString(context, "format");
        List<Executable> program = diskComponent.getProgram(stack);
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

        var pos = CommandUtils.parseBlockPos(posStr, source);
        BlockEntity blockEntity = source.getLevel().getBlockEntity(pos);

        if (!(blockEntity instanceof ShellContainer shell)) {
            throw INVALID_SLOT.create();
        }

        ItemStack disk = shell.getDiskStack();
        if (disk.isEmpty()) {
            throw NO_DISK.create();
        }
        SpellDiskComponent diskComponent = getDiskComponent(disk);
        if (diskComponent == null) {
            throw NO_DISK.create();
        }

        List<Executable> program = diskComponent.getProgram(disk);
        if (program.isEmpty()) {
            source.sendSuccess(() -> Component.literal("法术磁盘为空"), true);
            return 0;
        }

        sendFormattedProgram(source, program, format);
        return program.size();
    }

    /**
     * 从物品堆获取 SpellDiskComponent
     */
    private static SpellDiskComponent getDiskComponent(ItemStack stack) {
        if (stack.getItem() instanceof SpellDiskComponent) {
            return (SpellDiskComponent) stack.getItem();
        }
        return null;
    }

    /**
     * 按指定格式发送程序输出
     */
    private static void sendFormattedProgram(CommandSourceStack source, List<Executable> program, String format) {
        switch (format.toLowerCase()) {
            case "json" -> {
                String json = ProgramCompiler.toJsonString(program);
                source.sendSuccess(() -> CommandUtils.copyableText("JSON 程序 (§e" + program.size() + "§r 个指令): §f", json), true);
            }
            case "nbt" -> {
                try {
                    String nbt = ProgramCompiler.toNbt(program).toString();
                    source.sendSuccess(() -> CommandUtils.copyableText("NBT 程序 (§e" + program.size() + "§r 个指令): §f", nbt), true);
                } catch (ProgramCompiler.CompilationException e) {
                    source.sendFailure(Component.literal("§c NBT 序列化失败：" + e.getMessage()));
                }
            }
            default -> {
                source.sendSuccess(() -> CommandUtils.copyableText("法术程序 (§e" + program.size() + "§r 个指令): §f", CommandUtils.programToString(program)), true);
            }
        }
    }
}
