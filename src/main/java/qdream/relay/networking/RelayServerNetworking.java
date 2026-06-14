package qdream.relay.networking;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import qdream.relay.blocks.entity.custom.ShellBlockEntity;
import qdream.relay.core.ShellContainer;
import qdream.relay.networking.payloads.*;
import qdream.relay.mc.OperationRegistry;

/**
 * 服务端网络处理
 */
public class RelayServerNetworking {

    public static void register() {
        // 注册 C2S_ToggleShellPayload
        PayloadTypeRegistry.serverboundPlay().register(C2S_ToggleShellPayload.TYPE, C2S_ToggleShellPayload.CODEC);

        // 注册服务端接收处理器
        ServerPlayNetworking.registerGlobalReceiver(C2S_ToggleShellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null) return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.ShellScreenHandler handler) {
                    ShellContainer container = handler.getContainer();
                    if (container != null) {
                        container.setEnabled(!container.isEnabled());
                    }
                }
            });
        });
    }
    
    /**
     * 发送操作列表到客户端
     */
    public static void sendOperationList(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        List<String> ops = new ArrayList<>(OperationRegistry.getAllOperationIds());
        // TODO: 发送 S2C_OperationListPayload
    }
}
