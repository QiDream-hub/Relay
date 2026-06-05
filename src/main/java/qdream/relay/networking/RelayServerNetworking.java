package qdream.relay.networking;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import qdream.relay.networking.payloads.*;
import qdream.relay.engine.OperationRegistry;

/**
 * 服务端网络处理
 * 注意：26.1.2 版本的 Fabric API 网络系统有重大变化，暂时简化实现
 */
public class RelayServerNetworking {

    public static void register() {
        // 由于 26.1.2 的网络 API 有重大变化
        // 这里暂时不注册任何 payload
        // 等待 Fabric API 更新后再实现完整的网络同步
    }
    
    /**
     * 发送操作列表到客户端
     * 注意：需要使用 Fabric API 的网络系统
     */
    public static void sendOperationList(ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        List<String> ops = new ArrayList<>(OperationRegistry.getAllIds());
        // TODO: 使用 Fabric API 发送包
        // ServerPlayNetworking.send(player, new S2C_OperationListPayload(ops));
    }
}
