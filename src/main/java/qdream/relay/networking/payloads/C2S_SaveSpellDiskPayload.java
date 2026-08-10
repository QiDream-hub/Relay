package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：保存法术磁盘程序
 * 包含程序 JSON 字符串，服务端编译后保存到磁盘
 */
public record C2S_SaveSpellDiskPayload(String programJson) implements CustomPacketPayload {
    public static final Type<C2S_SaveSpellDiskPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_save_spell_disk")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_SaveSpellDiskPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, C2S_SaveSpellDiskPayload::programJson,
        C2S_SaveSpellDiskPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
