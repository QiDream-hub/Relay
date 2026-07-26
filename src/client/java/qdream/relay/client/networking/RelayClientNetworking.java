package qdream.relay.client.networking;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import qdream.relay.client.screen.SpellEditorScreen;
import qdream.relay.client.screen.ShellScreen;
import qdream.relay.engine.Executable;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.networking.payloads.S2C_ShellEnergyPayload;
import qdream.relay.networking.payloads.S2C_ShellLogPayload;
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
                if (mc.screen instanceof SpellEditorScreen editorScreen) {
                    try {
                        CompoundTag tag = payload.programNbt();
                        ListTag listTag = tag.getList("program").orElse(null);
                        if (listTag != null) {
                            List<Executable> program = ProgramCompiler.fromNbt(listTag);
                            editorScreen.updateProgramFromServer(program);
                        }
                    } catch (ProgramCompiler.CompilationException e) {
                        e.printStackTrace();
                    }
                }
            });
        });

        // 注册 S2C_ShellEnergyPayload 客户端接收器
        ClientPlayNetworking.registerGlobalReceiver(S2C_ShellEnergyPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null
                        && mc.player.containerMenu instanceof qdream.relay.screen.ShellScreenHandler handler) {
                    handler.setSyncedEnergy(payload.energy());
                }
            });
        });
        
        // 注册 S2C_ShellLogPayload 客户端接收器
        ClientPlayNetworking.registerGlobalReceiver(S2C_ShellLogPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null
                        && mc.player.containerMenu instanceof qdream.relay.screen.ShellScreenHandler handler) {
                    handler.setSyncedLogs(payload.logs());
                    // 通知 GUI 重绘
                    if (mc.screen instanceof ShellScreen shellScreen) {
                        if (shellScreen.getLogWidget() != null) {
                            shellScreen.getLogWidget().scrollToBottom();
                        }
                    }
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
