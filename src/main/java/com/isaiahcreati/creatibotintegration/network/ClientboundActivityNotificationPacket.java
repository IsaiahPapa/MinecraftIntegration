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
        int queuePosition,
        String iconType,
        String iconId
) implements CustomPacketPayload {

    public static final Type<ClientboundActivityNotificationPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CreatiIntegration.MODID, "activity_notification"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundActivityNotificationPacket> STREAM_CODEC =
            StreamCodec.of(ClientboundActivityNotificationPacket::encode, ClientboundActivityNotificationPacket::new);

    public ClientboundActivityNotificationPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readUtf(), buf.readUtf());
    }

    public ClientboundActivityNotificationPacket(String eventType, String tauntId, String redeemerName, String extraInfo, int queuePosition, String combinedIcon) {
        this(eventType, tauntId, redeemerName, extraInfo, queuePosition, splitIconType(combinedIcon), splitIconId(combinedIcon));
    }

    private static String splitIconType(String combined) {
        if (combined == null || combined.isEmpty()) return "";
        int idx = combined.indexOf(':');
        return idx < 0 ? combined : combined.substring(0, idx);
    }

    private static String splitIconId(String combined) {
        if (combined == null || combined.isEmpty()) return "";
        int idx = combined.indexOf(':');
        return idx < 0 ? "" : combined.substring(idx + 1);
    }

    public static void encode(RegistryFriendlyByteBuf buf, ClientboundActivityNotificationPacket packet) {
        buf.writeUtf(packet.eventType());
        buf.writeUtf(packet.tauntId());
        buf.writeUtf(packet.redeemerName());
        buf.writeUtf(packet.extraInfo());
        buf.writeVarInt(packet.queuePosition());
        buf.writeUtf(packet.iconType());
        buf.writeUtf(packet.iconId());
    }

    public static void handle(ClientboundActivityNotificationPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ActivityFeedState.addNotification(
                    packet.eventType(),
                    packet.tauntId(),
                    packet.redeemerName(),
                    packet.extraInfo(),
                    packet.queuePosition(),
                    packet.iconType(),
                    packet.iconId()
            );
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}