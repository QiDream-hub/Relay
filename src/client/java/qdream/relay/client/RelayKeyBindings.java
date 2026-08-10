package qdream.relay.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import qdream.relay.Relay;
import qdream.relay.items.ToolShellItem;
import qdream.relay.networking.payloads.C2S_OpenToolShellPayload;
import qdream.relay.networking.payloads.C2S_StopToolShellPayload;

/**
 * 客户端按键绑定注册
 * 使用 Fabric API 的 KeyMappingHelper 注册按键映射
 */
public class RelayKeyBindings {

    // 工具外壳配置按键（默认 B）
    public static KeyMapping OPEN_TOOL_SHELL_CONFIG;

    // 工具外壳停止程序按键（默认 X）
    public static KeyMapping STOP_TOOL_SHELL_PROGRAM;

    // 自定义分类
    public static KeyMapping.Category RELAY_CATEGORY;

    /**
     * 注册按键绑定和事件处理
     */
    public static void register() {
        // 步骤 1：注册自定义分类
        RELAY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Relay.MOD_ID, "relay")
        );

        // 步骤 2：使用 KeyMappingHelper 创建并注册按键映射
        OPEN_TOOL_SHELL_CONFIG = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.relay.open_tool_shell_config",  // 翻译键
                InputConstants.Type.KEYSYM,          // 键盘类型
                GLFW.GLFW_KEY_B,                     // B 键
                RELAY_CATEGORY                       // 所属分类
            )
        );

        STOP_TOOL_SHELL_PROGRAM = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                "key.relay.stop_tool_shell_program",  // 翻译键
                InputConstants.Type.KEYSYM,           // 键盘类型
                GLFW.GLFW_KEY_X,                      // X 键
                RELAY_CATEGORY                        // 所属分类
            )
        );

        // 步骤 3：注册按键按下事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            while (OPEN_TOOL_SHELL_CONFIG.consumeClick()) {
                // 检查玩家手持物品是否为工具外壳
                ItemStack mainHand = client.player.getItemInHand(InteractionHand.MAIN_HAND);
                ItemStack offHand = client.player.getItemInHand(InteractionHand.OFF_HAND);

                if (mainHand.getItem() instanceof ToolShellItem || offHand.getItem() instanceof ToolShellItem) {
                    // 发送网络包到服务端，让服务端打开 GUI
                    ClientPlayNetworking.send(new C2S_OpenToolShellPayload());
                }
            }

            while (STOP_TOOL_SHELL_PROGRAM.consumeClick()) {
                // 检查玩家手持物品是否为工具外壳
                ItemStack mainHand = client.player.getItemInHand(InteractionHand.MAIN_HAND);
                ItemStack offHand = client.player.getItemInHand(InteractionHand.OFF_HAND);

                if (mainHand.getItem() instanceof ToolShellItem || offHand.getItem() instanceof ToolShellItem) {
                    // 发送网络包到服务端，让服务端停止程序
                    ClientPlayNetworking.send(new C2S_StopToolShellPayload());
                }
            }
        });
    }
}
