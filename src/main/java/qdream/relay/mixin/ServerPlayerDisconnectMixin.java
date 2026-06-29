package qdream.relay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;

import qdream.relay.core.PlayerShellDataAccessor;

/**
 * 注入到 ServerPlayer.disconnect() 中，调用 PlayerShellData.clear()
 * 玩家下线时停止所有运行中的程序并保存状态
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerDisconnectMixin {

    @Inject(at = @At("TAIL"), method = "disconnect")
    private void relay$clearToolShells(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        if (player.level().isClientSide()) {
            return;
        }

        // 获取玩家的 PlayerShellData 并清空所有容器
        if (player instanceof PlayerShellDataAccessor accessor) {
            accessor.relay$getShellData().clear();
        }
    }
}
