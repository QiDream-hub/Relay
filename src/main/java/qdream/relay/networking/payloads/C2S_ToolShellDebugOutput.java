package qdream.relay.networking.payloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qdream.relay.Relay;

public record C2S_ToolShellDebugOutput(boolean enabled) implements CustomPacketPayload {
    public static final Type<C2S_ToolShellDebugOutput> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Relay.MOD_ID, "c2s_tool_shell_debug_output"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_ToolShellDebugOutput> CODEC =
        StreamCodec.ofMember(
            (payload, buf) -> buf.writeBoolean(payload.enabled()),
            buf -> new C2S_ToolShellDebugOutput(buf.readBoolean())
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
