package com.isaiahcreati.creatibotintegration.helpers;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.handlers.EventHandler;
import com.mojang.brigadier.CommandDispatcher;
import io.socket.client.Socket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.isaiahcreati.creatibotintegration.CreatiIntegration.LOGGER;
import static com.isaiahcreati.creatibotintegration.CreatiIntegration.socket;

@Mod.EventBusSubscriber
public class ModCommands {
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("start")
                .requires(source -> source.hasPermission(0))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    if(socket.isActive()){
                        player.sendSystemMessage(Component.literal("Socket already connected or trying to connect..."));
                        return 0;
                    }
                    if(!EventHandler.isConfigSetup()){
                        Chat.SendMessage(player, "You must configure the Mod's settings before connecting.");
                        return 0;
                    }
                    String ALERT_KEY = Config.ALERT_KEY.get();
                    Chat.SendMessage(player, "Starting game session...");
                    Chat.SendMessage(player, "Key: " + ALERT_KEY);
                    socket.connect();
                    socket.on(Socket.EVENT_CONNECT, args -> {
                        socket.emit("join", ALERT_KEY);
                        LOGGER.info("Connected to SocketIO");
                    });

                    return 1;
                })
        );
        dispatcher.register(Commands.literal("stop")
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
