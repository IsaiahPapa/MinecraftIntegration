package com.isaiahcreati.creatibotintegration.network;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CreatiIntegration.MODID, "taunt_effects"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(ClientboundTauntEffectPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ClientboundTauntEffectPacket::encode)
                .decoder(ClientboundTauntEffectPacket::new)
                .consumerNetworkThread(ClientboundTauntEffectPacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, ClientboundTauntEffectPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToAll(ClientboundTauntEffectPacket packet) {
        CHANNEL.send(PacketDistributor.ALL.with(() -> null), packet);
    }
}