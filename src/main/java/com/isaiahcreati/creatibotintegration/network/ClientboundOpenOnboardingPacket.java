package com.isaiahcreati.creatibotintegration.network;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.screens.OnboardingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClientboundOpenOnboardingPacket() implements CustomPacketPayload {

    public static final Type<ClientboundOpenOnboardingPacket> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(CreatiIntegration.MODID, "open_onboarding"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundOpenOnboardingPacket> STREAM_CODEC =
            StreamCodec.of(ClientboundOpenOnboardingPacket::encode, ClientboundOpenOnboardingPacket::new);

    public static void encode(RegistryFriendlyByteBuf buf, ClientboundOpenOnboardingPacket packet) {
    }

    public ClientboundOpenOnboardingPacket(RegistryFriendlyByteBuf buf) {
        this();
    }

    public static void handle(ClientboundOpenOnboardingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new OnboardingScreen());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}