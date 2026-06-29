package qdream.relay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import qdream.relay.core.PlayerShellDataAccessor;
import qdream.relay.items.ToolShellItem;
import qdream.relay.Component.RelayDataComponents;

/**
 * 注入到 ServerPlayer 加入世界时，扫描背包并恢复工具外壳会话
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerJoinMixin {

    @Inject(at = @At("TAIL"), method = "addAdditionalSaveData")
    private void relay$restoreToolShells(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        if (player.level().isClientSide()) {
            return;
        }

        // 获取玩家的 PlayerShellData
        if (player instanceof PlayerShellDataAccessor accessor) {
            var shellData = accessor.relay$getShellData();
            
            // 扫描玩家背包，恢复有会话 ID 的工具外壳
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ToolShellItem) {
                    // 检查是否有序会话 ID
                    String sessionIdStr = stack.get(RelayDataComponents.TOOL_SHELL_SESSION_ID);
                    if (sessionIdStr != null) {
                        try {
                            java.util.UUID sessionId = java.util.UUID.fromString(sessionIdStr);
                            // 从 ItemStack 加载状态并恢复 Container
                            shellData.restoreContainer(stack, sessionId);
                        } catch (IllegalArgumentException e) {
                            // UUID 格式错误，忽略
                        }
                    }
                }
            }
        }
    }
}
