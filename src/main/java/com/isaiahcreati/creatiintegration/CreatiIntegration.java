package com.isaiahcreati.creatiintegration;

import com.google.gson.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import io.socket.client.IO;
import io.socket.client.Socket;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
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

import java.util.List;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(CreatiIntegration.MOD_ID)
public class CreatiIntegration {
    public static final String MOD_ID = "creati_integration";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static Socket socket;

    private boolean started;


    public CreatiIntegration() {
        started = false;
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
            //TODO: Figure out a way to change this to production URL when building
            socket = IO.socket("ws://127.0.0.1:8006/integration");
            socket.on(Socket.EVENT_CONNECT, args -> {
                LOGGER.info("Connected to WebSocket");
            });

            socket.on("sys", args -> {
                ChatHelpers.Broadcast(args[0].toString());
            });

            socket.on("interaction:minecraft", args -> {
                try {
                    LOGGER.info("Got interaction: " + args.toString());
                    if (!(args[0] instanceof String jsonString)) {
                        ChatHelpers.Broadcast("Incorrect message type for interaction:minecraft. Should be String, got '" +  args[0].getClass().getName() + "'");
                        return;
                    }

                    Gson gson = new GsonBuilder().registerTypeAdapter(Payload.class, new PayloadDeserializer()).create();
                    Payload payload = gson.fromJson(jsonString, Payload.class);
                    ChatHelpers.Broadcast("[interaction:minecraft] Type '" +  payload.action + "'");
                    String fullPayloadJson = gson.toJson(payload);
                    LOGGER.info("Full Payload: " + fullPayloadJson);
                    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                        switch (payload.action) {
                            case GIVE:
                                if (!(payload.details instanceof ItemDetails itemDetails)) break;
                                Utils.givePlayerItem(player, itemDetails.itemId, itemDetails.amount);
                                player.sendSystemMessage(Component.literal("Trying to give item " + itemDetails.type + " of " + itemDetails.itemId), false);
                                break;
                            case SPAWN:
                                if (!(payload.details instanceof SpawnDetails spawnDetails)) break;
                                MobUtils.spawnMobNearPlayer(player, spawnDetails.mobId, spawnDetails.amount, payload.metadata.redeemerName);
                                player.sendSystemMessage(Component.literal("Trying to spawn " + spawnDetails.amount + " of " + spawnDetails.mobId), false);
                                break;
                            case EFFECT:
                                if (!(payload.details instanceof EffectDetails effectDetails)) break;
                                player.sendSystemMessage(Component.literal("Trying to spawn give " + player.getName() + " effect " + effectDetails.potionId), false);
                                Taunts.applyPotionEffect(player, effectDetails.potionId, effectDetails.duration, effectDetails.amplifier);
                                break;
                            case TAUNT:
                                if (!(payload.details instanceof TauntDetails tauntDetails)) break;
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
                                player.sendSystemMessage(Component.literal("Trying to taunt " + player.getName() + " with " + tauntDetails.tauntId), false);
                        }
                    }


                } catch (JsonSyntaxException e) {
                    LOGGER.error("Failed to spawn mob: " + e);
                    // Handle parsing error
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

    @Mod.EventBusSubscriber
    public class CommandRegistration {

        @SubscribeEvent
        public static void onCommandsRegister(RegisterCommandsEvent event) {
            CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

            dispatcher.register(Commands.literal("start")
                    .requires(source -> source.hasPermission(0))
                    .executes(context -> {
                        if(!socket.isActive()){
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            player.sendSystemMessage(Component.literal("SocketIO not connected :("));
                            return 0;
                        }
                        //TODO: make this alert ID configurable
                        socket.emit("join", "ae337b08-79a6-4eae-8228-5b7c14e8ee37");
                        return 1; // Return 1 to indicate success
                    })
            );
        }

    }

}