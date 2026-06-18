package com.isaiahcreati.creatibotintegration.network;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.client.ClientEffectManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundTauntEffectPacket(String effectId, int durationSeconds) implements CustomPacketPayload {

    public static final Type<ClientboundTauntEffectPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CreatiIntegration.MODID, "taunt_effects"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundTauntEffectPacket> STREAM_CODEC =
            StreamCodec.of(ClientboundTauntEffectPacket::encode, ClientboundTauntEffectPacket::new);

    public ClientboundTauntEffectPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readVarInt());
    }

    public static void encode(RegistryFriendlyByteBuf buf, ClientboundTauntEffectPacket packet) {
        buf.writeUtf(packet.effectId());
        buf.writeVarInt(packet.durationSeconds());
    }

    public static void handle(ClientboundTauntEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientEffectManager.activateEffect(packet.effectId(), packet.durationSeconds());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}