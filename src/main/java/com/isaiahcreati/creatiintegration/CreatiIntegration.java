package com.isaiahcreati.creatiintegration;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
        try{
            socket = IO.socket("http://localhost:3000");
            socket.on(Socket.EVENT_CONNECT, args -> LOGGER.info("Connected to WebSocket"));
            socket.on("spawn_mob", args -> {
                // Handle mob spawning
            });



            socket.on("timer", args -> {
                String message = (String) args[0];
                event.getServer().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
            });



            socket.on("spawn", args -> {
                String jsonString = (String) args[0];
                JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

                String mobId = jsonObject.get("mobId").getAsString();
                event.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Trying to spawn " + mobId), false);

                for(ServerPlayer player : event.getServer().getPlayerList().getPlayers()){
                    MobUtils.spawnMobNearPlayer(player, mobId);
                }
            });

            socket.on("taunt", args -> {
                String action = (String) args[0];

                for(ServerPlayer player : event.getServer().getPlayerList().getPlayers()){
                    if(action.equals("tnt")){
                        MobUtils.spawnPrimedTntOnPlayer(event.getServer().overworld(), player);
                    }
                    else if(action.equals("punch")){
                        MobUtils.smackPlayer(player);
                    }else if(action.equals("noise")){
                        MobUtils.playSuccessSound(player);
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
    public void onServerStopping(ServerStoppingEvent event){
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