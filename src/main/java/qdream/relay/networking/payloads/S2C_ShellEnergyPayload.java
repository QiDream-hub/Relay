package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;

/**
 * 服务端 → 客户端：同步 Shell 能量值
 */
public record S2C_ShellEnergyPayload(double energy) implements CustomPacketPayload {

    public static final Type<S2C_ShellEnergyPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(Relay.MOD_ID, "shell_energy")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_ShellEnergyPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE,
            S2C_ShellEnergyPayload::energy,
            S2C_ShellEnergyPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
