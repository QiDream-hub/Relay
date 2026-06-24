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
import qdream.relay.items.SpellDiskItem;

/**
 * 清空法术命令
 */
public class ClearCommands {

    private static final SimpleCommandExceptionType NO_DISK = new SimpleCommandExceptionType(
            Component.literal("手中没有法术磁盘")
    );

    private static final SimpleCommandExceptionType INVALID_SLOT = new SimpleCommandExceptionType(
            Component.literal("无效的插槽位置")
    );

    /**
     * 注册清空命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, LiteralCommandNode<CommandSourceStack> root) {
        // /relay clear <hand|shell> [slot]
        LiteralCommandNode<CommandSourceStack> clear = Commands.literal("clear")
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("hand");
                            builder.suggest("shell");
                            return builder.buildFuture();
                        })
                        .executes(ClearCommands::clearHand)
                        .then(Commands.literal("shell")
                                .then(Commands.argument("pos", StringArgumentType.word())
                                        .executes(ClearCommands::clearShell)
                                )
                        )
                )
                .build();

        root.addChild(clear);
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

        var pos = CommandUtils.parseBlockPos(posStr, source);
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
}
