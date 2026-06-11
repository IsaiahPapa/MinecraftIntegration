package com.isaiahcreati.creatibotintegration.helpers;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.handlers.EventHandler;
import com.isaiahcreati.creatibotintegration.integration.minigame.ParkourMinigame;
import com.isaiahcreati.creatibotintegration.integration.minigame.TntRunMinigame;
import com.mojang.brigadier.CommandDispatcher;
import io.socket.client.Socket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
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

        dispatcher.register(Commands.literal("creati")
                .then(Commands.literal("connect")
                        .requires(source -> source.hasPermission(0))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (socket.isActive()) {
                                player.sendSystemMessage(Component.literal("Socket already connected or trying to connect..."));
                                return 0;
                            }
                            if (!EventHandler.isConfigSetup()) {
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
                )
                .then(Commands.literal("disconnect")
                        .requires(source -> source.hasPermission(0))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (!socket.isActive()) {
                                player.sendSystemMessage(Component.literal("SocketIO not connected :("));
                                return 0;
                            }
                            socket.close();
                            player.sendSystemMessage(Component.literal("Disconnected from SocketIO").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55"))));
                            return 1;
                        })
                )
                .then(Commands.literal("parkour")
                        .then(Commands.literal("start")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ParkourMinigame parkour = CreatiIntegration.getParkourMinigame();
                                    if (parkour.isInMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are already in a parkour session!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    parkour.enterPlayer(player, "Dev");
                                    return 1;
                                })
                        )
                        .then(Commands.literal("leave")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ParkourMinigame parkour = CreatiIntegration.getParkourMinigame();
                                    if (!parkour.isInActiveMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are not in a parkour session!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    parkour.exitPlayer(player, true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("forceexit")
                                .requires(source -> source.hasPermission(3))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ParkourMinigame parkour = CreatiIntegration.getParkourMinigame();
                                    if (!parkour.isInMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("No active parkour session.").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55"))));
                                        return 0;
                                    }
                                    parkour.handlePlayerReconnect(player);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("tntrun")
                        .then(Commands.literal("start")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    TntRunMinigame tntrun = CreatiIntegration.getTntRunMinigame();
                                    if (tntrun.isInMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are already in a TNT Run session!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    tntrun.enterPlayer(player, "Dev");
                                    return 1;
                                })
                        )
                        .then(Commands.literal("leave")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    TntRunMinigame tntrun = CreatiIntegration.getTntRunMinigame();
                                    if (!tntrun.isInActiveMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are not in a TNT Run session!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    tntrun.exitPlayer(player, true);
                                    return 1;
                                })
                        )
                )
        );
    }
}