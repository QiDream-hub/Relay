package qdream.relay.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import qdream.relay.core.PlayerShellData;
import qdream.relay.core.PlayerShellDataAccessor;

/**
 * 为玩家附加 PlayerShellData
 * 使用 @Unique 字段存储玩家数据
 */
@Mixin(ServerPlayer.class)
public class PlayerShellDataMixin implements PlayerShellDataAccessor {

    @Unique
    @Final
    private PlayerShellData relay$shellData;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void relay$initShellData(CallbackInfo ci) {
        this.relay$shellData = new PlayerShellData((Player) (Object) this);
    }

    @Override
    @Unique
    public PlayerShellData relay$getShellData() {
        return relay$shellData;
    }
}
