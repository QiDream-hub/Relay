package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 服务端 → 客户端：确认法术磁盘保存成功
 */
public record S2C_SaveSpellDiskConfirmPayload(boolean success, String errorMessage) implements CustomPacketPayload {
    public static final Type<S2C_SaveSpellDiskConfirmPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Relay.MOD_ID, "s2c_save_spell_disk_confirm")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_SaveSpellDiskConfirmPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, S2C_SaveSpellDiskConfirmPayload::success,
            ByteBufCodecs.STRING_UTF8, S2C_SaveSpellDiskConfirmPayload::errorMessage,
            S2C_SaveSpellDiskConfirmPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
