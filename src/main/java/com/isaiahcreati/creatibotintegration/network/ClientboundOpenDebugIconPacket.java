package com.isaiahcreati.creatibotintegration.network;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.screens.DebugIconScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundOpenDebugIconPacket() implements CustomPacketPayload {

    public static final Type<ClientboundOpenDebugIconPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CreatiIntegration.MODID, "open_debug_icon"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenDebugIconPacket> STREAM_CODEC =
            StreamCodec.of(ClientboundOpenDebugIconPacket::encode, ClientboundOpenDebugIconPacket::new);

    public static void encode(RegistryFriendlyByteBuf buf, ClientboundOpenDebugIconPacket packet) {
    }

    public ClientboundOpenDebugIconPacket(RegistryFriendlyByteBuf buf) {
        this();
    }

    public static void handle(ClientboundOpenDebugIconPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new DebugIconScreen());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}