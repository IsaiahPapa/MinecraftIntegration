package com.isaiahcreati.creatibotintegration.helpers;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.handlers.EventHandler;
import com.isaiahcreati.creatibotintegration.integration.Taunt;
import com.isaiahcreati.creatibotintegration.integration.Taunts;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.socket.client.Socket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.isaiahcreati.creatibotintegration.CreatiIntegration.LOGGER;
import static com.isaiahcreati.creatibotintegration.CreatiIntegration.socket;

@Mod.EventBusSubscriber
public class ModCommands {

    private static final String[] SERVER_TAUNTS = {
            "tnt", "shuffle", "punch", "noise", "strike", "break", "wild", "drop", "cobweb",
            "drop_all", "half_heart", "hungry", "sky", "fake_tp", "jumpscare",
            "chicken_rain", "meteor_rain", "raid",
            "fire_trail", "downgrade_gear",
            "anvil", "bury", "curse_gear", "stack_one"
    };

    private static final String[] CLIENT_TAUNTS = {
            "fov_quake", "fov_zoom", "upside_down", "rolling_camera", "camera_tilt",
            "pumpkin_view", "dvd",
            "inverted_controls", "mouse_drifting",
            "blur", "inverted_colors", "black_and_white", "lsd", "crt"
    };

    private static final String[] MINIGAME_TAUNTS = {
            "parkour", "tntrun", "dropper"
    };

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
                            player.sendSystemMessage(Component.literal("Disconnected from SocketIO").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55"))));
                            return 1;
                        })
                )
                .then(Commands.literal("parkour")
                        .then(Commands.literal("start")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (CreatiIntegration.getParkourMinigame().isInMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are already in a parkour session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    CreatiIntegration.getParkourMinigame().enterPlayer(player, "Dev");
                                    return 1;
                                })
                        )
                        .then(Commands.literal("leave")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!CreatiIntegration.getParkourMinigame().isInActiveMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are not in a parkour session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    CreatiIntegration.getParkourMinigame().exitPlayer(player, true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("forceexit")
                                .requires(source -> source.hasPermission(3))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!CreatiIntegration.getParkourMinigame().isInMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("No active parkour session.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55"))));
                                        return 0;
                                    }
                                    CreatiIntegration.getParkourMinigame().handlePlayerReconnect(player);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("tntrun")
                        .then(Commands.literal("start")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (CreatiIntegration.getTntRunMinigame().isInMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are already in a TNT Run session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    CreatiIntegration.getTntRunMinigame().enterPlayer(player, "Dev");
                                    return 1;
                                })
                        )
                        .then(Commands.literal("leave")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!CreatiIntegration.getTntRunMinigame().isInActiveMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are not in a TNT Run session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    CreatiIntegration.getTntRunMinigame().exitPlayer(player, true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("dropper")
                        .then(Commands.literal("start")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (CreatiIntegration.getDropperMinigame().isInMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are already in a Dropper session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    CreatiIntegration.getDropperMinigame().enterPlayer(player, "Dev");
                                    return 1;
                                })
                        )
                        .then(Commands.literal("leave")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!CreatiIntegration.getDropperMinigame().isInActiveMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("You are not in a Dropper session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555"))));
                                        return 0;
                                    }
                                    CreatiIntegration.getDropperMinigame().exitPlayer(player, true);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("forceexit")
                                .requires(source -> source.hasPermission(3))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!CreatiIntegration.getDropperMinigame().isInMinigame(player)) {
                                        player.sendSystemMessage(Component.literal("No active Dropper session.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55"))));
                                        return 0;
                                    }
                                    CreatiIntegration.getDropperMinigame().handlePlayerReconnect(player);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("test")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            sendTestMenu(player);
                            return 1;
                        })
                        .then(Commands.argument("tauntId", StringArgumentType.word())
                                .requires(source -> source.hasPermission(2))
                                .suggests((context, builder) -> {
                                    for (String id : getAllTauntIds()) {
                                        builder.suggest(id);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String tauntId = StringArgumentType.getString(context, "tauntId");
                                    boolean success = TauntDispatcher.dispatchTaunt(player, tauntId, 5);
                                    if (success) {
                                        Taunt taunt = TauntDispatcher.getTaunts().getTauntById(tauntId);
                                        String name = taunt != null ? taunt.getDisplayName() : tauntId;
                                        Chat.SendAlert(player, "&7Test taunt: &b" + name);
                                    } else {
                                        Chat.SendAlert(player, "&cUnknown taunt: " + tauntId);
                                    }
                                    return success ? 1 : 0;
                                })
                        )
                        .then(Commands.literal("spawn")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("mobId", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String partial = builder.getRemaining().toLowerCase();
                                            for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
                                                ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(type);
                                                if (rl != null && rl.toString().toLowerCase().contains(partial)) {
                                                    builder.suggest(rl.toString());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String mobId = StringArgumentType.getString(context, "mobId");
                                            Mobs.spawnMobNearPlayer(player, mobId);
                                            Chat.SendAlert(player, "&7Spawned &b" + mobId);
                                            return 1;
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    String mobId = StringArgumentType.getString(context, "mobId");
                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                    Mobs.spawnMobNearPlayer(player, mobId, amount, "");
                                                    Chat.SendAlert(player, "&7Spawned &bx" + amount + " " + mobId);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("splash")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("effectId", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            String partial = builder.getRemaining().toLowerCase();
                                            for (MobEffect effect : ForgeRegistries.MOB_EFFECTS) {
                                                ResourceLocation rl = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                                                if (rl != null && rl.toString().toLowerCase().contains(partial)) {
                                                    builder.suggest(rl.toString());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String effectId = StringArgumentType.getString(context, "effectId");
                                            Taunts.applyPotionEffect(player, effectId, 15, 0);
                                            Chat.SendAlert(player, "&7Splashed &b" + effectId + " &7for &b15s");
                                            return 1;
                                        })
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 600))
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    String effectId = StringArgumentType.getString(context, "effectId");
                                                    int duration = IntegerArgumentType.getInteger(context, "duration");
                                                    Taunts.applyPotionEffect(player, effectId, duration, 0);
                                                    Chat.SendAlert(player, "&7Splashed &b" + effectId + " &7for &b" + duration + "s");
                                                    return 1;
                                                })
                                                .then(Commands.argument("amplifier", IntegerArgumentType.integer(0, 255))
                                                        .executes(context -> {
                                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                                            String effectId = StringArgumentType.getString(context, "effectId");
                                                            int duration = IntegerArgumentType.getInteger(context, "duration");
                                                            int amplifier = IntegerArgumentType.getInteger(context, "amplifier");
                                                            Taunts.applyPotionEffect(player, effectId, duration, amplifier);
                                                            Chat.SendAlert(player, "&7Splashed &b" + effectId + " " + Chat.NumberToRoman(amplifier + 1) + " &7for &b" + duration + "s");
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static List<String> getAllTauntIds() {
        List<String> ids = new ArrayList<>();
        for (String id : SERVER_TAUNTS) ids.add(id);
        for (String id : CLIENT_TAUNTS) ids.add(id);
        for (String id : MINIGAME_TAUNTS) ids.add(id);
        return ids;
    }

    private static void sendTestMenu(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00A76\u00A7l--- Creati's Taunt Test Menu ---"));
        player.sendSystemMessage(Component.literal("\u00A77Click a taunt to test it on yourself"));
        player.sendSystemMessage(Component.literal(""));

        player.sendSystemMessage(Component.literal("\u00A7e\u00A7lServer Taunts:"));
        sendTauntButtons(player, SERVER_TAUNTS, TextColor.parseColor("#FFAA00"));

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00A7b\u00A7lClient Effects:"));
        sendTauntButtons(player, CLIENT_TAUNTS, TextColor.parseColor("#55FFFF"));

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00A7a\u00A7lMinigames:"));
        sendTauntButtons(player, MINIGAME_TAUNTS, TextColor.parseColor("#55FF55"));

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00A78\u00A7m-------------------------------"));
        player.sendSystemMessage(Component.literal("\u00A77/creati test spawn \u00A7f<mobId> [amount]"));
        player.sendSystemMessage(Component.literal("\u00A77/creati test splash \u00A7f<effectId> [duration] [amplifier]"));
        player.sendSystemMessage(Component.literal("\u00A78\u00A7m-------------------------------"));
    }

    private static void sendTauntButtons(ServerPlayer player, String[] tauntIds, TextColor buttonColor) {
        Map<String, Taunt> allTaunts = TauntDispatcher.getTaunts().getAllTaunts();

        List<Component> buttons = new ArrayList<>();
        for (String id : tauntIds) {
            Taunt taunt = allTaunts.get(id);
            String displayName = taunt != null ? taunt.getDisplayName() : id;

            Component button = Component.literal("[" + displayName + "]")
                    .withStyle(Style.EMPTY
                            .withColor(buttonColor)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/creati test " + id))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to test: " + id)))
                    );
            buttons.add(button);
        }

        Component line = Component.literal("  ");
        for (int i = 0; i < buttons.size(); i++) {
            line = line.copy().append(buttons.get(i));
            if (i < buttons.size() - 1) {
                line = line.copy().append(Component.literal(" "));
            }
        }
        player.sendSystemMessage(line);
    }
}