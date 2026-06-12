package com.isaiahcreati.creatibotintegration.network;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.client.ActivityFeedState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundActivityNotificationPacket(
        String eventType,
        String tauntId,
        String redeemerName,
        String extraInfo,
        int queuePosition
) implements CustomPacketPayload {

    public static final Type<ClientboundActivityNotificationPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CreatiIntegration.MODID, "activity_notification"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundActivityNotificationPacket> STREAM_CODEC =
            StreamCodec.of(ClientboundActivityNotificationPacket::encode, ClientboundActivityNotificationPacket::new);

    public ClientboundActivityNotificationPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt());
    }

    public static void encode(RegistryFriendlyByteBuf buf, ClientboundActivityNotificationPacket packet) {
        buf.writeUtf(packet.eventType());
        buf.writeUtf(packet.tauntId());
        buf.writeUtf(packet.redeemerName());
        buf.writeUtf(packet.extraInfo());
        buf.writeVarInt(packet.queuePosition());
    }

    public static void handle(ClientboundActivityNotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ActivityFeedState.addNotification(
                    packet.eventType(),
                    packet.tauntId(),
                    packet.redeemerName(),
                    packet.extraInfo(),
                    packet.queuePosition()
            );
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}