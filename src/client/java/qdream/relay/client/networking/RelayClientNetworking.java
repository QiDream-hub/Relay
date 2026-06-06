package qdream.relay.client.networking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import qdream.relay.mc.OperationRegistry;

/**
 * 客户端网络处理
 * 注意：由于 26.1.2 网络 API 变更，暂时使用本地注册表同步
 */
public class RelayClientNetworking {

    private static List<String> availableOperations = new ArrayList<>();
    private static boolean isSynced = false;

    public static void register() {
        // 26.1.2 网络 API 变更，暂时不注册网络接收器
        // 使用本地同步方式获取操作列表
    }

    /**
     * 从服务端同步操作列表
     * 临时实现：直接从本地注册表获取
     */
    public static void syncOperations() {
        if (!isSynced) {
            Set<String> ops = OperationRegistry.getAllOperationIds();
            availableOperations = new ArrayList<>(ops);
            isSynced = true;
        }
    }

    /**
     * 设置可用的操作列表
     */
    public static void setAvailableOperations(List<String> operations) {
        availableOperations = new ArrayList<>(operations);
        isSynced = true;
    }

    /**
     * 获取可用的操作列表
     */
    public static List<String> getAvailableOperations() {
        syncOperations();
        return new ArrayList<>(availableOperations);
    }

    /**
     * 检查操作是否可用
     */
    public static boolean isOperationAvailable(String opId) {
        syncOperations();
        return availableOperations.contains(opId);
    }
}
