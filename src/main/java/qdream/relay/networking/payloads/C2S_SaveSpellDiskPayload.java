package qdream.relay.networking.payloads;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

/**
 * 客户端 → 服务端：保存法术磁盘程序
 * 包含程序 NBT 数据，服务端直接保存到磁盘而不经过 BlockEntity 缓存
 */
public record C2S_SaveSpellDiskPayload(CompoundTag programNbt) implements CustomPacketPayload {
    public static final Type<C2S_SaveSpellDiskPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_save_spell_disk")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_SaveSpellDiskPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.COMPOUND_TAG, C2S_SaveSpellDiskPayload::programNbt,
        C2S_SaveSpellDiskPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
