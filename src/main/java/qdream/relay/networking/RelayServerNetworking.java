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

        // 注册服务端接收处理器 - 磁盘插入时加载程序
        ServerPlayNetworking.registerGlobalReceiver(C2S_DiskInsertedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.SpellEditorScreenHandler handler) {
                    ItemStack diskStack = handler.getDiskItem();
                    if (!diskStack.isEmpty() && diskStack.getItem() instanceof DiskItem) {
                        handler.onDiskInserted(diskStack);
                        try {
                            ListTag programList = ProgramCompiler.toNbt(handler.getProgramEntries());
                            CompoundTag programTag = new CompoundTag();
                            programTag.put("program", programList);
                            ServerPlayNetworking.send(player, new S2C_SyncSpellDiskPayload(programTag));
                        } catch (ProgramCompiler.CompilationException e) {
                            e.printStackTrace();
                        }
                    }
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
                        blockEntity.setEnabled(!blockEntity.isEnabled());
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

        // 注册 C2S_SaveSpellDiskPayload
        PayloadTypeRegistry.serverboundPlay().register(C2S_SaveSpellDiskPayload.TYPE, C2S_SaveSpellDiskPayload.CODEC);

        // 注册服务端接收处理器 - 请求程序列表
        ServerPlayNetworking.registerGlobalReceiver(C2S_RequestProgramPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.SpellEditorScreenHandler handler) {
                    // 如果程序列表为空但磁盘存在，先加载磁盘
                    ItemStack diskStack = handler.getDiskItem();
                    if (handler.getProgramEntries().isEmpty() && !diskStack.isEmpty()
                            && diskStack.getItem() instanceof qdream.relay.items.DiskItem) {
                        handler.loadProgramFromDisk(diskStack);
                    }

                    try {
                        ListTag programList = ProgramCompiler.toNbt(handler.getProgramEntries());
                        CompoundTag programTag = new CompoundTag();
                        programTag.put("program", programList);
                        ServerPlayNetworking.send(player, new S2C_SyncSpellDiskPayload(programTag));
                    } catch (ProgramCompiler.CompilationException e) {
                        e.printStackTrace();
                    }
                }
            });
        });

        // 注册服务端接收处理器 - 保存法术磁盘（将实体程序保存到磁盘）
        ServerPlayNetworking.registerGlobalReceiver(C2S_SaveSpellDiskPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.SpellEditorScreenHandler handler) {
                    // 调用 handler.saveProgramToDisk() 将 blockEntity.program 保存到磁盘
                    handler.saveProgramToDisk();
                }
            });
        });

        // 注册 C2S_ProgramModifiedPayload
        PayloadTypeRegistry.serverboundPlay().register(C2S_ProgramModifiedPayload.TYPE,
                C2S_ProgramModifiedPayload.CODEC);

        // 注册服务端接收处理器 - 客户端程序修改
        ServerPlayNetworking.registerGlobalReceiver(C2S_ProgramModifiedPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player == null)
                return;

            context.server().execute(() -> {
                if (player.containerMenu instanceof qdream.relay.screen.SpellEditorScreenHandler handler) {
                    handler.onProgramModified(payload.programNbt());
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
    }
}
