package com.isaiahcreati.creatiintegration;

import com.google.gson.*;
import com.isaiahcreati.creatiintegration.handlers.EventHandler;
import com.isaiahcreati.creatiintegration.helpers.Chat;
import com.isaiahcreati.creatiintegration.helpers.Mobs;
import com.isaiahcreati.creatiintegration.helpers.Utils;
import com.isaiahcreati.creatiintegration.integration.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.socket.client.IO;
import io.socket.client.Socket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreatiIntegration.MOD_ID)
public class CreatiIntegration {
    public static final String MOD_ID = "creati_integration";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static Socket socket;

    private final Taunts taunts = new Taunts();

    public CreatiIntegration() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        // Load config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.CLIENT_CONFIG);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    private boolean isDevelopmentEnvironment(){
        return "development".equals(System.getenv("ENV"));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        try {
            String url;
            if (isDevelopmentEnvironment()) {
                url = "ws://127.0.0.1:8006/integration";
            } else {
                url = "wss://alerts.isaiahcreati.com/integration";
            }

            socket = IO.socket(url);
            socket.on(Socket.EVENT_CONNECT, args -> {
                LOGGER.info("Connected to WebSocket");
            });

            socket.on("sys", args -> {
                Chat.Broadcast(args[0].toString());
            });

            socket.on("interaction:minecraft", args -> {
                try {
                    LOGGER.info("Got interaction: " + args.toString());
                    if (!(args[0] instanceof String jsonString)) {
                        Chat.Broadcast("Incorrect message type for interaction:minecraft. Should be String, got '" +  args[0].getClass().getName() + "'");
                        return;
                    }

                    Gson gson = new GsonBuilder().registerTypeAdapter(Payload.class, new PayloadDeserializer()).create();
                    Payload payload = gson.fromJson(jsonString, Payload.class);
                    String fullPayloadJson = gson.toJson(payload);
                    LOGGER.info("Full Payload: " + fullPayloadJson);
                    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                        switch (payload.action) {
                            case GIVE:
                                if (!(payload.details instanceof ItemDetails itemDetails)) break;
                                Taunts.givePlayerItem(player, itemDetails.itemId, itemDetails.amount);
                                Item item = Utils.getItemById(itemDetails.itemId);

                                Chat.SendAlert(player, "&b" + payload.metadata.redeemerName + "&7 gave you &bx" + itemDetails.amount + " " + item.getDescription().getString());
                                break;
                            case SPAWN:
                                if (!(payload.details instanceof SpawnDetails spawnDetails)) break;
                                Mobs.spawnMobNearPlayer(player, spawnDetails.mobId, spawnDetails.amount, payload.metadata.redeemerName);
                                EntityType mob = Mobs.getMobByName(spawnDetails.mobId);
                                Chat.SendAlert(player, "&b" + payload.metadata.redeemerName + "&7 spawned &bx" + spawnDetails.amount + " " + mob.getDescription().getString());
                                break;
                            case EFFECT:
                                if (!(payload.details instanceof EffectDetails effectDetails)) break;

                                Taunts.applyPotionEffect(player, effectDetails.potionId, effectDetails.duration, effectDetails.amplifier);
                                MobEffect effect = Utils.getPotionEffect(effectDetails.potionId);
                                //TODO: Fix this so it prints out the potion info
                                Chat.SendAlert(player, "&b" + payload.metadata.redeemerName + "&7 splashed you with &b" + effect.getDisplayName().getString() + " " + Chat.NumberToRoman(effectDetails.amplifier + 1) + "!");
                                break;
                            case TAUNT:
                                if (!(payload.details instanceof TauntDetails tauntDetails)) break;
                                Taunt taunt = taunts.getTauntById(tauntDetails.tauntId);
                                switch (tauntDetails.tauntId) {
                                    case "tnt" -> Taunts.spawnPrimedTntOnPlayer(event.getServer().overworld(), player);
                                    case "shuffle" -> Taunts.ShuffleInventory(player);
                                    case "punch" -> Taunts.smackPlayer(player);
                                    case "noise" -> Utils.playSoundByName(player, "CREEPER_PRIME");
                                    case "strike" -> Taunts.strikeDownPlayer(player);
                                    case "break" -> Taunts.breakBlockUnderPlayer(player);
                                    case "wild" -> Taunts.teleportPlayerToRandomLocation(player);
                                    case "drop" -> Taunts.dropHand(player);
                                    case "cobweb" -> Taunts.webBlockPlayer(player);
                                }
                                Chat.SendAlert(player, "&b" + payload.metadata.redeemerName + "&7 taunted you with &b" + taunt.getDisplayName());
                        }
                    }
                    // TODO: on reconnect attempts, send user chat-message updates.


                } catch (JsonSyntaxException e) {
                    LOGGER.error("Failed process interaction: " + e);
                    e.printStackTrace();
                }
            });

            socket.on(Socket.EVENT_DISCONNECT, args -> LOGGER.info("Disconnected from WebSocket"));
            socket.connect();
        } catch (Exception e) {
            LOGGER.error("Something errored in SocketIO", e);
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    static void setupClient(final FMLClientSetupEvent event) {
        LOGGER.info("Setting up client...");

    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping...");
        if (socket != null) {
            LOGGER.info("Disconnecting from SocketIO Server...");
            socket.disconnect();
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Get the username of the player who logged in
        String playerName = event.getEntity().getDisplayName().getString();

        // Create the join message
        String joinMessage = playerName + " has joined the server! Welcome!";
        event.getEntity().getServer().getPlayerList().broadcastSystemMessage(Component.literal(joinMessage), false);
    }

    @Mod.EventBusSubscriber
    public class CommandRegistration {

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

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        message = message.replace('&', '§');
        event.setMessage(Component.literal(message));
    }

}