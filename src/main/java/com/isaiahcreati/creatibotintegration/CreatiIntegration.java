package com.isaiahcreati.creatibotintegration;

import com.google.gson.*;
import com.isaiahcreati.creatibotintegration.handlers.EventHandler;
import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.helpers.Mobs;
import com.isaiahcreati.creatibotintegration.helpers.TauntDispatcher;
import com.isaiahcreati.creatibotintegration.helpers.Utils;
import com.isaiahcreati.creatibotintegration.integration.*;
import com.isaiahcreati.creatibotintegration.integration.minigame.DropperMinigame;
import com.isaiahcreati.creatibotintegration.integration.minigame.Minigame;
import com.isaiahcreati.creatibotintegration.integration.minigame.MinigameEventHandler;
import com.isaiahcreati.creatibotintegration.integration.minigame.ParkourMinigame;
import com.isaiahcreati.creatibotintegration.integration.minigame.TntRunMinigame;
import com.isaiahcreati.creatibotintegration.network.PacketHandler;
import com.isaiahcreati.creatibotintegration.screens.ModConfigScreen;
import com.mojang.logging.LogUtils;
import io.socket.client.IO;
import io.socket.client.Socket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.ConfigScreenHandler;
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


@Mod("creatibotintegration")
public class CreatiIntegration {
    public static final String MODID = "creatibotintegration";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static Socket socket;
    private int reconnectAttempts = 0;

    private final Taunts taunts = new Taunts();
    private static final ParkourMinigame parkourMinigame = new ParkourMinigame();
    private static final TntRunMinigame tntRunMinigame = new TntRunMinigame();
    private static final DropperMinigame dropperMinigame = new DropperMinigame();

    public static ParkourMinigame getParkourMinigame() { return parkourMinigame; }
    public static TntRunMinigame getTntRunMinigame() { return tntRunMinigame; }
    public static DropperMinigame getDropperMinigame() { return dropperMinigame; }


    public CreatiIntegration() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
        MinigameEventHandler.registerMinigame(parkourMinigame);
        MinigameEventHandler.registerMinigame(tntRunMinigame);
        MinigameEventHandler.registerMinigame(dropperMinigame);
        MinecraftForge.EVENT_BUS.register(new MinigameEventHandler());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.CLIENT_CONFIG);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> ModConfigScreen.create(screen)));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);

        if (Config.needsReset()) {
            LOGGER.info("Config version outdated, resetting to defaults...");
            Config.resetToDefaults();
        }
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
                reconnectAttempts = 0;
                LOGGER.info("Connected to SocketIO");
            });
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                LOGGER.info("Disconnected from SocketIO");
                for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    Chat.SendMessage(player, "Disconnected from Server, attempting to reconnect");
                }
            });
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                LOGGER.info("Failed to connect to server");
                reconnectAttempts++;
                if(reconnectAttempts >= 5){
                    reconnectAttempts = 0;
                    socket.disconnect();
                    LOGGER.info("Failed to connect to server. Please try again later.");
                    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                        Chat.SendMessage(player, "Failed to connect to server. Please try again later");
                    }
                    return;
                }
                for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    Chat.SendMessage(player, "Failed to connect to Creati's Inegration Server. Attempt #" + reconnectAttempts);
                }
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
                                boolean dispatched = TauntDispatcher.dispatchTaunt(player, tauntDetails.tauntId);
                                if (taunt != null && dispatched) {
                                    Chat.SendAlert(player, "&b" + payload.metadata.redeemerName + "&7 taunted you with &b" + taunt.getDisplayName());
                                }
                        }
                    }
                    // TODO: on reconnect attempts, send user chat-message updates.


                } catch (JsonSyntaxException e) {
                    LOGGER.error("Failed process interaction: " + e);
                    e.printStackTrace();
                }
            });
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
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        message = message.replace('&', '§');
        event.setMessage(Component.literal(message));
    }

}