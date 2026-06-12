package com.isaiahcreati.creatibotintegration.network;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.client.ClientQueueState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record ClientboundQueueUpdatePacket(
        List<QueueEntry> minigameQueue,
        List<QueueEntry> visualEffectQueue,
        String activeMinigameId,
        String activeMinigameRedeemer,
        String activeVisualEffectId,
        String activeVisualEffectRedeemer,
        int activeVisualEffectRemainingSeconds,
        int activeVisualEffectDurationSeconds,
        int pausedEffectRemainingSeconds,
        int pendingTauntsCount
) implements CustomPacketPayload {

    public static final Type<ClientboundQueueUpdatePacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CreatiIntegration.MODID, "queue_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundQueueUpdatePacket> STREAM_CODEC =
            StreamCodec.of(ClientboundQueueUpdatePacket::encode, ClientboundQueueUpdatePacket::new);

    public ClientboundQueueUpdatePacket(RegistryFriendlyByteBuf buf) {
        this(
                readQueueEntries(buf),
                readQueueEntries(buf),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void encode(RegistryFriendlyByteBuf buf, ClientboundQueueUpdatePacket packet) {
        writeQueueEntries(buf, packet.minigameQueue());
        writeQueueEntries(buf, packet.visualEffectQueue());
        buf.writeUtf(packet.activeMinigameId() != null ? packet.activeMinigameId() : "");
        buf.writeUtf(packet.activeMinigameRedeemer() != null ? packet.activeMinigameRedeemer() : "");
        buf.writeUtf(packet.activeVisualEffectId() != null ? packet.activeVisualEffectId() : "");
        buf.writeUtf(packet.activeVisualEffectRedeemer() != null ? packet.activeVisualEffectRedeemer() : "");
        buf.writeVarInt(packet.activeVisualEffectRemainingSeconds());
        buf.writeVarInt(packet.activeVisualEffectDurationSeconds());
        buf.writeVarInt(packet.pausedEffectRemainingSeconds());
        buf.writeVarInt(packet.pendingTauntsCount());
    }

    private static List<QueueEntry> readQueueEntries(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<QueueEntry> entries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            entries.add(new QueueEntry(buf.readUtf(), buf.readUtf(), buf.readVarInt()));
        }
        return entries;
    }

    private static void writeQueueEntries(RegistryFriendlyByteBuf buf, List<QueueEntry> entries) {
        buf.writeVarInt(entries.size());
        for (QueueEntry entry : entries) {
            buf.writeUtf(entry.tauntId());
            buf.writeUtf(entry.redeemerName());
            buf.writeVarInt(entry.durationSeconds());
        }
    }

    public static void handle(ClientboundQueueUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientQueueState.updateFromPacket(packet);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record QueueEntry(String tauntId, String redeemerName, int durationSeconds) {}
}