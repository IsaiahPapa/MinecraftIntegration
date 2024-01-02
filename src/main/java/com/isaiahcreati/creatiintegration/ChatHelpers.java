package com.isaiahcreati.creatiintegration;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class ChatHelpers {

    static public void Broadcast(String message){
        // Get the MinecraftServer instance
        MinecraftServer minecraftServer = ServerLifecycleHooks.getCurrentServer();
        // Send chat message
        minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal(message), false);

    }
}
