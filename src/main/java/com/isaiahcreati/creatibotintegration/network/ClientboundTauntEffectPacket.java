package com.isaiahcreati.creatibotintegration.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientboundTauntEffectPacket {
    private final String effectId;
    private final int durationSeconds;

    public ClientboundTauntEffectPacket(String effectId, int durationSeconds) {
        this.effectId = effectId;
        this.durationSeconds = durationSeconds;
    }

    public ClientboundTauntEffectPacket(FriendlyByteBuf buf) {
        this.effectId = buf.readUtf();
        this.durationSeconds = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(effectId);
        buf.writeVarInt(durationSeconds);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            com.isaiahcreati.creatibotintegration.client.ClientEffectManager.activateEffect(effectId, durationSeconds);
        });
        ctx.get().setPacketHandled(true);
    }

    public String getEffectId() {
        return effectId;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }
}