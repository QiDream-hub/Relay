package qdream.relay.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import qdream.relay.engine.Executable;
import qdream.relay.mc.base.Operation;
import qdream.relay.types.BooleanType;
import qdream.relay.types.ListType;
import qdream.relay.types.NumberType;
import qdream.relay.types.StringType;

import java.util.List;

/**
 * 命令工具类
 */
public class CommandUtils {

    private static final SimpleCommandExceptionType INVALID_SLOT = new SimpleCommandExceptionType(
            Component.literal("无效的插槽位置")
    );

    /**
     * 解析方块坐标字符串
     */
    public static BlockPos parseBlockPos(String posStr, CommandSourceStack source) throws CommandSyntaxException {
        String[] parts = posStr.split(",");
        if (parts.length != 3) {
            throw INVALID_SLOT.create();
        }
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            return new BlockPos(x, y, z);
        } catch (NumberFormatException e) {
            throw INVALID_SLOT.create();
        }
    }

    /**
     * 构建带点击复制功能的文本组件
     * 点击后内容会被填入聊天输入框，方便玩家复制
     */
    public static MutableComponent copyableText(String prefix, String content) {
        MutableComponent label = Component.literal(prefix);
        MutableComponent body = Component.literal(content)
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.CopyToClipboard(content))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal("§7 点击复制到剪贴板")))
                        .withUnderlined(true)
                );
        return label.append(body);
    }

    /**
     * 将程序转换为可读字符串
     */
    public static String programToString(List<Executable> program) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < program.size(); i++) {
            Executable exec = program.get(i);
            if (i > 0) sb.append("; ");

            if (exec instanceof Operation op) {
                String str = op.getId();
                if (str.startsWith("relay:")) {
                    sb.append(str.substring(6));
                } else {
                    sb.append("\"").append(str).append("\"");
                }
            } else if (exec instanceof StringType s) {
                sb.append("\"").append(s.asString()).append("\"");
            } else if (exec instanceof NumberType n) {
                sb.append(n.getValue());
            } else if (exec instanceof BooleanType b) {
                sb.append(b.asBoolean());
            } else if (exec instanceof ListType list) {
                sb.append("[...]");
            } else {
                sb.append(((Operation) exec).getId());
            }
        }
        return sb.toString();
    }

    /**
     * 将数据栈转换为可读字符串
     */
    public static String dataStackToString(List<Executable> dataStack) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dataStack.size(); i++) {
            Executable data = dataStack.get(i);
            if (i > 0) sb.append(", ");

            if (data instanceof StringType s) {
                sb.append("\"").append(s.asString()).append("\"");
            } else if (data instanceof NumberType n) {
                sb.append(n.getValue());
            } else if (data instanceof BooleanType b) {
                sb.append(b.asBoolean());
            } else {
                sb.append(((Operation) data).getId());
            }
        }
        return sb.toString();
    }
}
