package com.isaiahcreati.creatiintegration.helpers;

import com.isaiahcreati.creatiintegration.Config;
import com.isaiahcreati.creatiintegration.screens.ConfigScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import static com.isaiahcreati.creatiintegration.CreatiIntegration.socket;

@Mod.EventBusSubscriber
public class ModCommands {
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("start")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if(!socket.isActive()){
                        player.sendSystemMessage(Component.literal("SocketIO not connected :("));
                        return 0;
                    }
                    String ALERT_KEY = Config.ALERT_KEY.get();
                    if(!ALERT_KEY.isEmpty()){
                        Chat.SendMessage(player, "Starting game session...");
                        Chat.SendMessage(player, "Key: " + ALERT_KEY);
                        socket.emit("join", ALERT_KEY);
                    }
                    return 1;
                })
        );
        dispatcher.register(Commands.literal("start")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if(!socket.isActive()){
                        player.sendSystemMessage(Component.literal("SocketIO not connected :("));
                        return 0;
                    }
                    socket.close();
                    return 1; // Return 1 to indicate success
                })
        );
    }
}
