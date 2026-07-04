package qdream.relay.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Relay 模组命令注册入口
 */
public class RelayCommands {

    /**
     * 注册所有命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("relay").build();
        dispatcher.getRoot().addChild(root);

        // 注册各子命令模块
        WriteCommands.register(dispatcher, root);
        ReadCommands.register(dispatcher, root);
        ClearCommands.register(dispatcher, root);
    }
}
