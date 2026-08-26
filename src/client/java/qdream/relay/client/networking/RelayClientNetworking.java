package qdream.relay.client.networking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import qdream.relay.client.screen.EditorScreen;
import qdream.relay.client.screen.BlockShellScreen;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.networking.payloads.S2C_ShellEnergyPayload;
import qdream.relay.networking.payloads.S2C_ShellLogPushPayload;
import qdream.relay.networking.payloads.S2C_ClearLogsPayload;
import qdream.relay.networking.payloads.S2C_SyncSpellDiskPayload;

/**
 * 客户端网络处理
 */
public class RelayClientNetworking {

    private static List<String> availableOperations = new ArrayList<>();
    private static boolean isSynced = false;

    public static void register() {
        // 注册 S2C_SyncSpellDiskPayload 客户端接收器（payload 类型已在 RelayServerNetworking 中注册）
        ClientPlayNetworking.registerGlobalReceiver(S2C_SyncSpellDiskPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof EditorScreen editorScreen) {
                    editorScreen.updateProgramFromServer(payload.programJson());
                }
            });
        });

        // 注册 S2C_ShellEnergyPayload 客户端接收器
        ClientPlayNetworking.registerGlobalReceiver(S2C_ShellEnergyPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null
                        && mc.player.containerMenu instanceof qdream.relay.screen.BlockShellScreenHandler handler) {
                    handler.setSyncedEnergy(payload.energy());
                }
            });
        });

        // 注册 S2C_ShellLogPushPayload 客户端接收器 - 单条日志实时推送
        ClientPlayNetworking.registerGlobalReceiver(S2C_ShellLogPushPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                // 使用当前玩家所在世界的维度
                Minecraft mc = Minecraft.getInstance();
                Level level = mc.player != null ? mc.player.level() : mc.level;
                if (level != null) {
                    ClientLogCacheManager.addLog(level, payload.pos(), payload.log());
                }
            });
        });

        // 注册 S2C_ClearLogsPayload 客户端接收器 - 方块破坏时清理缓存
        ClientPlayNetworking.registerGlobalReceiver(S2C_ClearLogsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                // 使用当前玩家所在世界的维度
                Minecraft mc = Minecraft.getInstance();
                Level level = mc.player != null ? mc.player.level() : mc.level;
                if (level != null) {
                    ClientLogCacheManager.clearLogs(level, payload.pos());
                }
            });
        });
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
