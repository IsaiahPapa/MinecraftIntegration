package com.isaiahcreati.creatibotintegration.network;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ServerboundOnboardingPacket(String alertKey, boolean chatAlerts, boolean minigamesEnabled, boolean sidebarVisible, boolean activityFeedVisible) implements CustomPacketPayload {

    public static final Type<ServerboundOnboardingPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CreatiIntegration.MODID, "onboarding"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerboundOnboardingPacket> STREAM_CODEC =
            StreamCodec.of(ServerboundOnboardingPacket::encode, ServerboundOnboardingPacket::new);

    public ServerboundOnboardingPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void encode(RegistryFriendlyByteBuf buf, ServerboundOnboardingPacket packet) {
        buf.writeUtf(packet.alertKey());
        buf.writeBoolean(packet.chatAlerts());
        buf.writeBoolean(packet.minigamesEnabled());
        buf.writeBoolean(packet.sidebarVisible());
        buf.writeBoolean(packet.activityFeedVisible());
    }

    public static void handle(ServerboundOnboardingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Config.ALERT_KEY.set(packet.alertKey());
            Config.CHAT_ALERTS.set(packet.chatAlerts());
            Config.PARKOUR_ENABLED.set(packet.minigamesEnabled());
            Config.TNT_RUN_ENABLED.set(packet.minigamesEnabled());
            Config.DROPPER_ENABLED.set(packet.minigamesEnabled());
            Config.SIDEBAR_VISIBLE.set(packet.sidebarVisible());
            Config.ACTIVITY_FEED_VISIBLE.set(packet.activityFeedVisible());
            Config.ONBOARDED.set(true);
            Config.CLIENT_CONFIG.save();
            CreatiIntegration.LOGGER.info("Onboarding completed by player. Config saved.");
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}