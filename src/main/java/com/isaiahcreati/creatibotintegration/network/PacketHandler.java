package com.isaiahcreati.creatibotintegration.network;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1")
                .versioned("1.0")
                .optional();

        registrar.playToClient(
                ClientboundTauntEffectPacket.TYPE,
                ClientboundTauntEffectPacket.STREAM_CODEC,
                ClientboundTauntEffectPacket::handle
        );

        registrar.playToClient(
                ClientboundOpenOnboardingPacket.TYPE,
                ClientboundOpenOnboardingPacket.STREAM_CODEC,
                ClientboundOpenOnboardingPacket::handle
        );

        registrar.playToServer(
                ServerboundOnboardingPacket.TYPE,
                ServerboundOnboardingPacket.STREAM_CODEC,
                ServerboundOnboardingPacket::handle
        );
    }

    public static void sendToPlayer(ServerPlayer player, ClientboundTauntEffectPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void sendToAll(ClientboundTauntEffectPacket packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }

    public static void sendOnboardingScreen(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ClientboundOpenOnboardingPacket());
    }
}