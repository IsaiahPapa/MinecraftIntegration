package com.isaiahcreati.creatiintegration;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import io.socket.client.IO;
import io.socket.client.Socket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.json.JSONObject;
import org.slf4j.Logger;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreatiIntegration.MOD_ID)
public class CreatiIntegration {
    public static final String MOD_ID = "creati_integration";
    public static final Logger LOGGER = LogUtils.getLogger();
    private Socket socket;


    public CreatiIntegration() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);

        //Register Mod Entities
//        ModEntities.REGISTER.register(modEventBus);

        JsonArray array = new JsonArray();

        for (String effectName : ForgeRegistries.MOB_EFFECTS.getKeys().stream().map(val -> val.getPath()).toList()) {
            array.add(effectName);
        }
        LOGGER.info("Types of effects: " + array);


    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        try {
            socket = IO.socket("http://localhost:3000");
            socket.on(Socket.EVENT_CONNECT, args -> LOGGER.info("Connected to WebSocket"));
            socket.on("spawn_mob", args -> {
                // Handle mob spawning
            });

            socket.on("timer", args -> {
                String message = args[0].toString();
                event.getServer().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
            });

            socket.on("effect", args -> {
                String message = args[0].toString();
                for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    Taunts.applyPotionEffect(player, message);
                }
            });

            socket.on("spawn", args -> {
                try {
                    if (!(args[0] instanceof String jsonString)) {
                        event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Failed to spawn mob. jsonString is null"), false);
                        return;
                    }
                    SpawnPayload payload = new SpawnPayload(jsonString);

                    event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Trying to spawn " + payload.amount + " of " + payload.mobId), false);

                    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                        MobUtils.spawnMobNearPlayer(player, payload.mobId, payload.amount);
                    }

                } catch (JsonSyntaxException e) {
                    LOGGER.error("Failed to spawn mob: " + e);
                    // Handle parsing error
                }
            });

            socket.on("taunt", args -> {
                String action = (String) args[0];

                for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    switch (action) {
                        case "tnt" -> MobUtils.spawnPrimedTntOnPlayer(event.getServer().overworld(), player);
                        case "fakeTnt" -> {
//                        MobUtils.spawnFakePrimedTntOnPlayer(event.getServer().overworld(), player);
                        }
                        case "shuffle" -> Taunts.ShuffleInventory(player);
                        case "punch" -> MobUtils.smackPlayer(player);
                        case "noise" -> MobUtils.playSuccessSound(player);
                        case "strike" -> Taunts.strikeDownPlayer(player);
                        case "break" -> Taunts.breakBlockUnderPlayer(player);
                        case "wild" -> Taunts.teleportPlayerToRandomLocation(player);
                    }
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

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

}