package com.isaiahcreati.creatiintegration.helpers;

import com.isaiahcreati.creatiintegration.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import static java.util.Collections.nCopies;
import static org.openjdk.nashorn.internal.objects.NativeArray.join;

public final class Chat {

    static public void Broadcast(String message){
        // Get the MinecraftServer instance
        MinecraftServer minecraftServer = ServerLifecycleHooks.getCurrentServer();
        // Send chat message
        minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }

    static public void SendMessage(ServerPlayer player, String message){
        // Get the MinecraftServer instance
        MinecraftServer minecraftServer = ServerLifecycleHooks.getCurrentServer();

        //Replace character to support colors
        message = message.replace('&', '§');

        // Send chat message
        minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }

    static public void SendAlert(ServerPlayer player, String message){
        boolean CHAT_ALERTS = Config.CHAT_ALERTS.get();
        if(!CHAT_ALERTS) return;
        SendMessage(player, "&f[Creati's Bot] &r" + message);
    }

    static public String NumberToRoman(int number) {
        String[] romanSymbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

        StringBuilder roman = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                roman.append(romanSymbols[i]);
            }
        }

        return roman.toString();
    }

}
