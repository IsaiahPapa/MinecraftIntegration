package com.isaiahcreati.creatibotintegration.helpers;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.handlers.EventHandler;
import com.isaiahcreati.creatibotintegration.integration.QueueManager;
import com.isaiahcreati.creatibotintegration.integration.Taunt;
import com.isaiahcreati.creatibotintegration.integration.Taunts;
import com.isaiahcreati.creatibotintegration.helpers.SafeMode;
import com.isaiahcreati.creatibotintegration.network.PacketHandler;
import com.isaiahcreati.creatibotintegration.network.ClientboundActivityNotificationPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.socket.client.Socket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.isaiahcreati.creatibotintegration.CreatiIntegration.LOGGER;
import static com.isaiahcreati.creatibotintegration.CreatiIntegration.socket;

@EventBusSubscriber(modid = CreatiIntegration.MODID)
public class ModCommands {

    private static final String[] SERVER_TAUNTS = {
            "tnt", "shuffle", "punch", "noise", "strike", "break", "wild", "drop", "cobweb",
            "drop_all", "half_heart", "hungry", "sky", "fake_tp", "jumpscare",
            "chicken_rain", "meteor_rain", "raid",
            "fire_trail", "downgrade_gear",
            "anvil", "bury", "curse_gear", "stack_one",
            "mob_army", "anvil_rain", "blind_noise",
            "rename_chat", "hot_potato", "lucky_block"
    };

    private static final String[] CLIENT_TAUNTS = {
            "fov_quake", "fov_zoom", "upside_down", "rolling_camera", "camera_tilt",
            "pumpkin_view", "dvd",
            "inverted_controls", "mouse_drifting",
            "blur", "inverted_colors", "black_and_white", "lsd", "crt",
            "drunk", "vignette_heartbeat",
            "pixelate", "mirror", "fisheye"
    };

    private static final String[] MINIGAME_TAUNTS = {
            "parkour", "tntrun", "dropper"
    };

    private static final Permission HAS_OP = new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS);

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("creati")
                // /creati connect
                .then(Commands.literal("connect")
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
                // /creati disconnect
                .then(Commands.literal("disconnect")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (!socket.isActive()) {
                                player.sendSystemMessage(Component.literal("SocketIO not connected :("));
                                return 0;
                            }
                            socket.close();
                            player.sendSystemMessage(Component.literal("Disconnected from SocketIO").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55").getOrThrow())));
                            return 1;
                        })
                )
                // /creati safemode <on|off> [seconds]
                .then(Commands.literal("safemode")
                        .then(Commands.literal("on")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    SafeMode.enable(context.getSource().getServer(), 30);
                                    Chat.SendMessage(player, "\u00a7d\u00a7l\u2696 Safe Mode \u00a7r\u00a77enabled for \u00a7d30s\u00a77. Redeems are paused.");
                                    return 1;
                                })
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int seconds = IntegerArgumentType.getInteger(context, "seconds");
                                            SafeMode.enable(context.getSource().getServer(), seconds);
                                            Chat.SendMessage(player, "\u00a7d\u00a7l\u2696 Safe Mode \u00a7r\u00a77enabled for \u00a7d" + seconds + "s\u00a77. Redeems are paused.");
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("off")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!SafeMode.isActive()) {
                                        Chat.SendMessage(player, "\u00a77Safe Mode is not currently active.");
                                        return 0;
                                    }
                                    SafeMode.disable();
                                    Chat.SendMessage(player, "\u00a7a\u00a7l\u2714 Safe Mode \u00a7r\u00a77disabled. Redeems are live again.");
                                    return 1;
                                })
                        )
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            if (SafeMode.isActive()) {
                                Chat.SendMessage(player, "\u00a7d\u00a7l\u2696 Safe Mode \u00a7r\u00a77is active: \u00a7d" + SafeMode.getRemainingSeconds() + "s\u00a77 remaining");
                            } else {
                                Chat.SendMessage(player, "\u00a77Safe Mode is \u00a7coff\u00a77. Usage: /creati safemode <on|off> [seconds]");
                            }
                            return 1;
                        })
                )
                // /creati setup
                .then(Commands.literal("setup")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PacketHandler.sendOnboardingScreen(player);
                            return 1;
                        })
                )
                // /creati book
                .then(Commands.literal("book")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ItemStack book = OnboardingBook.create();
                            if (!player.getInventory().add(book)) {
                                player.spawnAtLocation((ServerLevel) player.level(), book);
                            }
                            player.sendSystemMessage(Component.literal("Gave you the onboarding book!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#55AAFF").getOrThrow())));
                            return 1;
                        })
                )
                // /creati debug queue
                // /creati debug icon
                .then(Commands.literal("debug")
                        .then(Commands.literal("queue")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    if (!Config.QUEUE_ENABLED.get()) {
                                        player.sendSystemMessage(Component.literal("Queue system is disabled.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow())));
                                        return 0;
                                    }
                                    player.sendSystemMessage(Component.literal("\u00a76\u00a7l--- Queue Status ---"));
                                    String mg = QueueManager.getActiveMinigameId();
                                    if (mg != null && !mg.isEmpty()) {
                                        String redeemer = QueueManager.getActiveMinigameRedeemer();
                                        player.sendSystemMessage(Component.literal("\u00a7aActive minigame: \u00a7f" + mg + (redeemer != null ? " (by " + redeemer + ")" : "")));
                                    } else {
                                        player.sendSystemMessage(Component.literal("\u00a78No active minigame"));
                                    }
                                    String ve = QueueManager.getActiveVisualEffectId();
                                    if (ve != null && !ve.isEmpty()) {
                                        player.sendSystemMessage(Component.literal("\u00a7eActive effect: \u00a7f" + ve));
                                    } else {
                                        player.sendSystemMessage(Component.literal("\u00a78No active effect"));
                                    }
                                    player.sendSystemMessage(Component.literal("\u00a7bMinigame queue: \u00a7f" + QueueManager.getMinigameQueueSize()));
                                    player.sendSystemMessage(Component.literal("\u00a7bEffect queue: \u00a7f" + QueueManager.getVisualEffectQueueSize()));
                                    player.sendSystemMessage(Component.literal("\u00a7bPending taunts: \u00a7f" + QueueManager.getPendingTauntsSize()));
                                    return 1;
                                })
                        )
                        .then(Commands.literal("icon")
                                .requires(source -> source.permissions().hasPermission(HAS_OP))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    PacketHandler.sendDebugIconScreen(player);
                                    return 1;
                                })
                        )
                )
                // /creati test (op)
                .then(Commands.literal("test")
                        .requires(source -> source.permissions().hasPermission(HAS_OP))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            sendTestMenu(player);
                            return 1;
                        })
                        // /creati test taunt <id>
                        .then(Commands.literal("taunt")
                                .then(Commands.argument("tauntId", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            for (String id : getAllTauntIds()) {
                                                builder.suggest(id);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String tauntId = StringArgumentType.getString(context, "tauntId");
                                            if (Config.QUEUE_ENABLED.get()) {
                                                QueueManager.enqueue(player, tauntId, "Test", 15);
                                                Taunt taunt = TauntDispatcher.getTaunts().getTauntById(tauntId);
                                                String name = taunt != null ? taunt.getDisplayName() : tauntId;
                                                Chat.SendAlert(player, "&7Test taunt (queued): &b" + name);
                                                return 1;
                                            }
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
                        )
                        // /creati test spawn <mob> [amount]
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("mobId", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            String partial = builder.getRemaining().toLowerCase();
                                            for (Identifier rl : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                                                if (rl.toString().toLowerCase().contains(partial)) {
                                                    builder.suggest(rl.toString());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String mobId = StringArgumentType.getString(context, "mobId");
                                            String[] parts = mobId.split(" ");
                                            String actualMobId = parts[0];
                                            int amount = 1;
                                            if (parts.length > 1) {
                                                try { amount = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { amount = 1; }
                                            }
                                            Mobs.spawnMobNearPlayer(player, actualMobId, amount, "");
                                            String mobName = actualMobId;
                                            var entityType = net.minecraft.world.entity.EntityType.byString(actualMobId);
                                            if (entityType.isPresent()) {
                                                mobName = entityType.get().getDescription().getString();
                                            }
                                            Chat.SendAlert(player, "&7Spawned &bx" + amount + " " + mobName);
                                            String icon = ToastIconHelper.getIconForAction("SPAWN", actualMobId.contains(":") ? actualMobId : "minecraft:" + actualMobId);
                                            PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("SPAWN", "", "Test", amount + "x " + mobName, 0, icon));
                                            return 1;
                                        })
                                )
                        )
                        // /creati test potion <effect> [duration] [amplifier]
                        .then(Commands.literal("potion")
                                .then(Commands.argument("effectId", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            String partial = builder.getRemaining().toLowerCase();
                                            for (Identifier rl : BuiltInRegistries.MOB_EFFECT.keySet()) {
                                                if (rl.toString().toLowerCase().contains(partial)) {
                                                    builder.suggest(rl.toString());
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String effectId = StringArgumentType.getString(context, "effectId");
                                            String[] parts = effectId.split(" ");
                                            String actualEffectId = parts[0];
                                            int duration = 15;
                                            int amplifier = 0;
                                            if (parts.length > 1) {
                                                try { duration = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { duration = 15; }
                                            }
                                            if (parts.length > 2) {
                                                try { amplifier = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { amplifier = 0; }
                                            }
                                            Taunts.applyPotionEffect(player, actualEffectId, duration, amplifier);
                                            Chat.SendAlert(player, "&7Splashed &b" + actualEffectId + " &7for &b" + duration + "s");
                                            PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("EFFECT", "", "Test", actualEffectId, 0, ToastIconHelper.getIconForAction("EFFECT", "")));
                                            return 1;
                                        })
                                )
                        )
                        // /creati test minigame <parkour|tntrun|dropper> <start|leave|forceexit>
                        .then(Commands.literal("minigame")
                                .then(Commands.literal("parkour")
                                        .then(Commands.literal("start")
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    if (CreatiIntegration.getParkourMinigame().isInMinigame(player)) {
                                                        player.sendSystemMessage(Component.literal("You are already in a parkour session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow())));
                                                        return 0;
                                                    }
                                                    CreatiIntegration.getParkourMinigame().enterPlayer(player, "Dev");
                                                    return 1;
                                                })
                                        )
                                        .then(Commands.literal("leave")
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    if (!CreatiIntegration.getParkourMinigame().isInActiveMinigame(player)) {
                                                        player.sendSystemMessage(Component.literal("You are not in a parkour session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow())));
                                                        return 0;
                                                    }
                                                    CreatiIntegration.getParkourMinigame().exitPlayer(player, true);
                                                    return 1;
                                                })
                                        )
                                        .then(Commands.literal("forceexit")
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    if (!CreatiIntegration.getParkourMinigame().isInMinigame(player)) {
                                                        player.sendSystemMessage(Component.literal("No active parkour session.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55").getOrThrow())));
                                                        return 0;
                                                    }
                                                    CreatiIntegration.getParkourMinigame().handlePlayerReconnect(player);
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("tntrun")
                                        .then(Commands.literal("start")
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    if (CreatiIntegration.getTntRunMinigame().isInMinigame(player)) {
                                                        player.sendSystemMessage(Component.literal("You are already in a TNT Run session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow())));
                                                        return 0;
                                                    }
                                                    CreatiIntegration.getTntRunMinigame().enterPlayer(player, "Dev");
                                                    return 1;
                                                })
                                        )
                                        .then(Commands.literal("leave")
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    if (!CreatiIntegration.getTntRunMinigame().isInActiveMinigame(player)) {
                                                        player.sendSystemMessage(Component.literal("You are not in a TNT Run session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow())));
                                                        return 0;
                                                    }
                                                    CreatiIntegration.getTntRunMinigame().exitPlayer(player, true);
                                                    return 1;
                                                })
                                        )
                                )
                                .then(Commands.literal("dropper")
                                        .then(Commands.literal("start")
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    if (CreatiIntegration.getDropperMinigame().isInMinigame(player)) {
                                                        player.sendSystemMessage(Component.literal("You are already in a Dropper session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow())));
                                                        return 0;
                                                    }
                                                    CreatiIntegration.getDropperMinigame().enterPlayer(player, "Dev");
                                                    return 1;
                                                })
                                        )
                                        .then(Commands.literal("leave")
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    if (!CreatiIntegration.getDropperMinigame().isInActiveMinigame(player)) {
                                                        player.sendSystemMessage(Component.literal("You are not in a Dropper session!").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow())));
                                                        return 0;
                                                    }
                                                    CreatiIntegration.getDropperMinigame().exitPlayer(player, true);
                                                    return 1;
                                                })
                                        )
                                        .then(Commands.literal("forceexit")
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    if (!CreatiIntegration.getDropperMinigame().isInMinigame(player)) {
                                                        player.sendSystemMessage(Component.literal("No active Dropper session.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55").getOrThrow())));
                                                        return 0;
                                                    }
                                                    CreatiIntegration.getDropperMinigame().handlePlayerReconnect(player);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        // /creati test notify <type> [name] [redeemer] [position]
                        .then(Commands.literal("notify")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("GIVE");
                                            builder.suggest("SPAWN");
                                            builder.suggest("EFFECT");
                                            builder.suggest("TAUNT_INSTANT");
                                            builder.suggest("TAUNT_QUEUED");
                                            builder.suggest("TAUNT_ACTIVATED");
                                            builder.suggest("TAUNT_EXTENDED");
                                            builder.suggest("MINIGAME_QUEUED");
                                            builder.suggest("MINIGAME_ACTIVATED");
                                            builder.suggest("VISUAL_EFFECT_QUEUED");
                                            builder.suggest("VISUAL_EFFECT_ACTIVATED");
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String type = StringArgumentType.getString(context, "type");
                                            PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket(type, "tnt", "TestUser", "Test extra info", 0, ToastIconHelper.getIconForTaunt("tnt")));
                                            player.sendSystemMessage(Component.literal("Sent activity notification: " + type).withStyle(Style.EMPTY.withColor(TextColor.parseColor("#55FF55").getOrThrow())));
                                            return 1;
                                        })
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    String type = StringArgumentType.getString(context, "type");
                                                    String name = StringArgumentType.getString(context, "name");
                                                    String icon = ToastIconHelper.getIconForTaunt(name);
                                                    PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket(type, name, "TestUser", "", 0, icon));
                                                    player.sendSystemMessage(Component.literal("Sent activity notification: " + type + " " + name).withStyle(Style.EMPTY.withColor(TextColor.parseColor("#55FF55").getOrThrow())));
                                                    return 1;
                                                })
                                                .then(Commands.argument("redeemer", StringArgumentType.word())
                                                        .executes(context -> {
                                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                                            String type = StringArgumentType.getString(context, "type");
                                                            String name = StringArgumentType.getString(context, "name");
                                                            String redeemer = StringArgumentType.getString(context, "redeemer");
                                                            String icon = ToastIconHelper.getIconForTaunt(name);
                                                            PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket(type, name, redeemer, "", 0, icon));
                                                            player.sendSystemMessage(Component.literal("Sent activity notification: " + type + " " + name + " by " + redeemer).withStyle(Style.EMPTY.withColor(TextColor.parseColor("#55FF55").getOrThrow())));
                                                            return 1;
                                                        })
                                                        .then(Commands.argument("position", IntegerArgumentType.integer(0))
                                                                .executes(context -> {
                                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                                    String type = StringArgumentType.getString(context, "type");
                                                                    String name = StringArgumentType.getString(context, "name");
                                                                    String redeemer = StringArgumentType.getString(context, "redeemer");
                                                                    int pos = IntegerArgumentType.getInteger(context, "position");
                                                                    String icon = ToastIconHelper.getIconForTaunt(name);
                                                                    PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket(type, name, redeemer, "", pos, icon));
                                                                    player.sendSystemMessage(Component.literal("Sent activity notification: " + type + " " + name + " by " + redeemer + " #" + pos).withStyle(Style.EMPTY.withColor(TextColor.parseColor("#55FF55").getOrThrow())));
                                                                    return 1;
                                                                })
                                                        )
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
        sendTauntButtons(player, SERVER_TAUNTS, TextColor.parseColor("#FFAA00").getOrThrow());

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00A7b\u00A7lClient Effects:"));
        sendTauntButtons(player, CLIENT_TAUNTS, TextColor.parseColor("#55FFFF").getOrThrow());

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00A7a\u00A7lMinigames:"));
        sendTauntButtons(player, MINIGAME_TAUNTS, TextColor.parseColor("#55FF55").getOrThrow());

        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("\u00A78\u00A7m-------------------------------"));
        player.sendSystemMessage(Component.literal("\u00a77/creati test spawn \u00a7f<mobId> [amount]"));
        player.sendSystemMessage(Component.literal("\u00a77/creati test potion \u00a7f<effectId> [duration] [amplifier]"));
        player.sendSystemMessage(Component.literal("\u00a77/creati test minigame \u00a7f<parkour|tntrun|dropper> <start|leave|forceexit>"));
        player.sendSystemMessage(Component.literal("\u00a77/creati test notify \u00a7f<type> [name] [redeemer] [position]"));
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
                            .withClickEvent(new ClickEvent.RunCommand("/creati test taunt " + id))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(Component.literal("Click to test: " + id)))
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