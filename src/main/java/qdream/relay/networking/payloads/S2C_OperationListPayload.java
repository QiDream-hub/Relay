package qdream.relay.networking.payloads;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import qdream.relay.Relay;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 → 客户端：操作列表
 */
public record S2C_OperationListPayload(List<String> operationIds) implements CustomPacketPayload {
    public static final Type<S2C_OperationListPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "s2c_operation_list"));
    public static final StreamCodec<FriendlyByteBuf, S2C_OperationListPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
            S2C_OperationListPayload::operationIds,
            S2C_OperationListPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
