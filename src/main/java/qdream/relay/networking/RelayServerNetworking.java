package qdream.relay.networking;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import qdream.relay.Component.RelayDataComponents;
import qdream.relay.blocks.entity.custom.BlockShellEntity;
import qdream.relay.core.ShellContainer;
import qdream.relay.items.DiskItem;
import qdream.relay.networking.payloads.*;
import qdream.relay.mc.OperationRegistry;
import qdream.relay.mc.ProgramCompiler;
import qdream.relay.mc.ProgramCompiler.CompilationException;
import qdream.relay.Relay;

/**
 * 服务端网络处理
 */
public class RelayServerNetworking {

    public static void register() {
        // 注册 S2C_ShellEnergyPayload (服务端到客户端)
        PayloadTypeRegistry.clientboundPlay().register(S2C_ShellEnergyPayload.TYPE, S2C_ShellEnergyPayload.CODEC);

        // 注册 S2C_ShellLogPayload (服务端到客户端 - 日志同步)
        PayloadTypeRegistry.clientboundPlay().register(S2C_ShellLogPayload.TYPE, S2C_ShellLogPayload.CODEC);

        // 注册 S2C_SyncSpellDiskPayload (服务端到客户端)
        PayloadTypeRegistry.clientboundPlay().register(S2C_SyncSpellDiskPayload.TYPE, S2C_SyncSpellDiskPayload.CODEC);

        // 注册 C2S_RequestProgramPayload (客户端到服务端)
        PayloadTypeRegistry.serverboundPlay().register(C2S_RequestProgramPayload.TYPE, C2S_RequestProgramPayload.CODEC);

        // 注册 C2S_DiskInsertedPayload (客户端到服务端)
        PayloadTypeRegistry.serverboundPlay().register(C2S_DiskInsertedPayload.TYPE, C2S_DiskInsertedPayload.CODEC);

        // 注册服务端接收处理器 - 客户端请求加载磁盘
        ServerPlayNetworking.registerGlobalReceiver(C2S_DiskInsertedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.EditorScreenHandler handler) {
                    handler.onDiskInserted();
                }
            });
        });

        // 注册 C2S_ToggleShellPayload
        PayloadTypeRegistry.serverboundPlay().register(C2S_ToggleShellPayload.TYPE, C2S_ToggleShellPayload.CODEC);

        // 注册服务端接收处理器 - 切换开关
        ServerPlayNetworking.registerGlobalReceiver(C2S_ToggleShellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.ShellScreenHandler handler) {
                    qdream.relay.blocks.entity.custom.BlockShellEntity blockEntity = handler.getBlockEntity();
                    if (blockEntity != null) {
                        blockEntity.setEnabled(!blockEntity.isEnabled()&&blockEntity.isInitialized());
                    }
                }
            });
        });

        // 注册 C2S_InitializeShellPayload - 复位外壳（清空双栈并从磁盘加载程序）
        PayloadTypeRegistry.serverboundPlay().register(C2S_InitializeShellPayload.TYPE,
                C2S_InitializeShellPayload.CODEC);

        // 注册服务端接收处理器 - 复位程序
        ServerPlayNetworking.registerGlobalReceiver(C2S_InitializeShellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.ShellScreenHandler handler) {
                    qdream.relay.blocks.entity.custom.BlockShellEntity blockEntity = handler.getBlockEntity();
                    if (blockEntity != null) {
                        blockEntity.loadProgramFromDisk();
                    }
                }
            });
        });

        // 注册 C2S_SaveSpellDiskPayload - 保存程序到磁盘（包含程序 NBT 数据）
        PayloadTypeRegistry.serverboundPlay().register(C2S_SaveSpellDiskPayload.TYPE, C2S_SaveSpellDiskPayload.CODEC);

        // 注册服务端接收处理器 - 请求程序列表
        ServerPlayNetworking.registerGlobalReceiver(C2S_RequestProgramPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.EditorScreenHandler handler) {
                    // 从磁盘加载并同步到客户端
                    ItemStack diskStack = handler.getDiskItem();
                    if (!diskStack.isEmpty() && diskStack.getItem() instanceof qdream.relay.items.DiskItem) {
                        handler.loadProgramFromDisk(diskStack);
                    }
                }
            });
        });

        // 注册 S2C_SaveSpellDiskConfirmPayload (服务端到客户端 - 保存确认)
        PayloadTypeRegistry.clientboundPlay().register(S2C_SaveSpellDiskConfirmPayload.TYPE, S2C_SaveSpellDiskConfirmPayload.CODEC);

        // 注册服务端接收处理器 - 保存法术磁盘（将 JSON 字符串编译后保存到磁盘）
        ServerPlayNetworking.registerGlobalReceiver(C2S_SaveSpellDiskPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.EditorScreenHandler handler) {
                    // 编译 JSON 并保存到磁盘
                    try {
                        ProgramCompiler.compileFromJson(payload.programJson());
                        // 编译通过，直接保存 JSON 字符串
                        ItemStack diskStack = handler.getDiskItem();
                        if (!diskStack.isEmpty() && diskStack.getItem() instanceof qdream.relay.mc.component.DiskComponent diskComponent) {
                            diskComponent.setProgram(diskStack, payload.programJson());
                            // 发送保存成功确认到客户端
                            ServerPlayNetworking.send(player, new S2C_SaveSpellDiskConfirmPayload(true, ""));
                        } else {
                            // 磁盘不存在或不是法术磁盘
                            ServerPlayNetworking.send(player, new S2C_SaveSpellDiskConfirmPayload(false, "没有有效的法术磁盘"));
                        }
                    } catch (CompilationException e) {
                        Relay.LOGGER.error("服务端编译失败：" + e.getMessage());
                        // 发送编译失败确认到客户端
                        ServerPlayNetworking.send(player, new S2C_SaveSpellDiskConfirmPayload(false, e.getMessage()));
                    }
                }
            });
        });

        // 注册 C2S_OpenToolShellPayload
        PayloadTypeRegistry.serverboundPlay().register(C2S_OpenToolShellPayload.TYPE, C2S_OpenToolShellPayload.CODEC);

        // 注册服务端接收处理器 - 打开工具外壳 GUI
        ServerPlayNetworking.registerGlobalReceiver(C2S_OpenToolShellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                // 检查玩家手持物品
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();

                if (mainHand.getItem() instanceof qdream.relay.items.ToolShellItem toolShellItem) {
                    player.openMenu(new qdream.relay.items.menu.ToolShellMenuProvider(mainHand));
                } else if (offHand.getItem() instanceof qdream.relay.items.ToolShellItem toolShellItem) {
                    player.openMenu(new qdream.relay.items.menu.ToolShellMenuProvider(offHand));
                }
            });
        });

        // 注册 C2S_ToolShellConfigPayload - 统一配置更新
        PayloadTypeRegistry.serverboundPlay().register(C2S_ToolShellConfigPayload.TYPE,
                C2S_ToolShellConfigPayload.CODEC);

        // 注册服务端接收处理器 - 工具外壳配置更新（统一处理所有配置项）
        ServerPlayNetworking.registerGlobalReceiver(C2S_ToolShellConfigPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.ToolShellScreenHandler handler) {
                    // 统一设置所有配置项
                    handler.setUseInventoryEnergyModule(payload.useInventoryEnergyModule());
                    handler.setDebugOutputEnabled(payload.debugOutputEnabled());
                    handler.setStatusInfo(payload.statusInfoEnabled());
                }
            });
        });

        // 注册 C2S_RequestShellLogPayload - 客户端请求日志同步
        PayloadTypeRegistry.serverboundPlay().register(C2S_RequestShellLogPayload.TYPE,
                C2S_RequestShellLogPayload.CODEC);

        // 注册服务端接收处理器 - 请求日志同步
        ServerPlayNetworking.registerGlobalReceiver(C2S_RequestShellLogPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.ShellScreenHandler handler) {
                    qdream.relay.blocks.entity.custom.BlockShellEntity blockEntity = handler.getBlockEntity();
                    if (blockEntity != null) {
                        // 每 10 tick 同步一次日志
                        if (blockEntity.getLevel().getGameTime() % 10 == 0) {
                            blockEntity.syncLogsToClient(blockEntity.getLevel(), blockEntity.getBlockPos());
                        }
                    }
                }
            });
        });

        // 注册 C2S_StopToolShellPayload - 客户端请求停止工具外壳程序
        PayloadTypeRegistry.serverboundPlay().register(C2S_StopToolShellPayload.TYPE, C2S_StopToolShellPayload.CODEC);

        // 注册服务端接收处理器 - 停止工具外壳程序
        ServerPlayNetworking.registerGlobalReceiver(C2S_StopToolShellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                // 检查玩家手持物品
                ItemStack mainHand = player.getMainHandItem();
                ItemStack offHand = player.getOffhandItem();

                if (mainHand.getItem() instanceof qdream.relay.items.ToolShellItem) {
                    qdream.relay.items.container.ToolShellContainer container = getToolShellContainer(mainHand, player);
                    if (container != null) {
                        container.getStateMachine().clear();
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[工具外壳] 程序已停止"));
                    }
                } else if (offHand.getItem() instanceof qdream.relay.items.ToolShellItem) {
                    qdream.relay.items.container.ToolShellContainer container = getToolShellContainer(offHand, player);
                    if (container != null) {
                        container.getStateMachine().clear();
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[工具外壳] 程序已停止"));
                    }
                }
            });
        });
    }

    /**
     * 获取工具外壳容器
     */
    private static qdream.relay.items.container.ToolShellContainer getToolShellContainer(ItemStack stack, ServerPlayer player) {
        if (player instanceof qdream.relay.core.PlayerShellDataAccessor accessor) {
            return accessor.relay$getShellData().getContainer(stack);
        }
        return null;
    }
}
