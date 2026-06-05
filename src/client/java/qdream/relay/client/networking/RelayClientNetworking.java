package qdream.relay.client.networking;

import java.util.ArrayList;
import java.util.List;

import qdream.relay.networking.payloads.S2C_OperationListPayload;

/**
 * 客户端网络处理
 */
public class RelayClientNetworking {
    
    private static List<String> availableOperations = new ArrayList<>();
    
    public static void register() {
        // 注册 S2C 操作列表的处理
        // 注意：26.1.2 版本的 Fabric API 网络系统有重大变化
        // 暂时不注册，等待 API 更新
        
        // 伪代码示例：
        // PayloadTypeRegistry.registerS2C(S2C_OperationListPayload.TYPE, S2C_OperationListPayload.CODEC);
        // ClientPlayNetworking.registerGlobalReceiver(S2C_OperationListPayload.TYPE, (payload, context) -> {
        //     availableOperations = payload.operationIds();
        // });
    }
    
    /**
     * 设置可用的操作列表
     */
    public static void setAvailableOperations(List<String> operations) {
        availableOperations = new ArrayList<>(operations);
    }
    
    /**
     * 获取可用的操作列表
     */
    public static List<String> getAvailableOperations() {
        return new ArrayList<>(availableOperations);
    }
    
    /**
     * 检查操作是否可用
     */
    public static boolean isOperationAvailable(String opId) {
        return availableOperations.contains(opId);
    }
}
