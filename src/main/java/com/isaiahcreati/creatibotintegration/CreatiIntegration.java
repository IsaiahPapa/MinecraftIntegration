package com.isaiahcreati.creatibotintegration;

import com.google.gson.*;
import com.isaiahcreati.creatibotintegration.handlers.EventHandler;
import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.helpers.Mobs;
import com.isaiahcreati.creatibotintegration.helpers.OnboardingBook;
import com.isaiahcreati.creatibotintegration.helpers.SafeMode;
import com.isaiahcreati.creatibotintegration.helpers.TauntDispatcher;
import com.isaiahcreati.creatibotintegration.helpers.Utils;
import com.isaiahcreati.creatibotintegration.integration.*;
import com.isaiahcreati.creatibotintegration.integration.minigame.DropperMinigame;
import com.isaiahcreati.creatibotintegration.integration.minigame.Minigame;
import com.isaiahcreati.creatibotintegration.integration.minigame.MinigameEventHandler;
import com.isaiahcreati.creatibotintegration.integration.minigame.ParkourMinigame;
import com.isaiahcreati.creatibotintegration.integration.minigame.SumoMinigame;
import com.isaiahcreati.creatibotintegration.integration.minigame.TntRunMinigame;
import com.isaiahcreati.creatibotintegration.helpers.ToastIconHelper;
import com.isaiahcreati.creatibotintegration.network.ClientboundActivityNotificationPacket;
import com.isaiahcreati.creatibotintegration.network.PacketHandler;
import com.isaiahcreati.creatibotintegration.screens.ModConfigScreen;
import com.mojang.logging.LogUtils;
import io.socket.client.IO;
import io.socket.client.Socket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
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
    private static final SumoMinigame sumoMinigame = new SumoMinigame();

    public static ParkourMinigame getParkourMinigame() { return parkourMinigame; }
    public static TntRunMinigame getTntRunMinigame() { return tntRunMinigame; }
    public static DropperMinigame getDropperMinigame() { return dropperMinigame; }
    public static SumoMinigame getSumoMinigame() { return sumoMinigame; }


    public CreatiIntegration(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(PacketHandler::register);
        modEventBus.addListener(com.isaiahcreati.creatibotintegration.client.ClientModEventHandler::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.register(this);
        MinigameEventHandler.registerMinigame(parkourMinigame);
        MinigameEventHandler.registerMinigame(tntRunMinigame);
        MinigameEventHandler.registerMinigame(dropperMinigame);
        MinigameEventHandler.registerMinigame(sumoMinigame);
        NeoForge.EVENT_BUS.register(new MinigameEventHandler());
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.CLIENT_CONFIG);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (java.util.function.Supplier<IConfigScreenFactory>) () -> (container, screen) -> ModConfigScreen.create(screen));
    }

    private void commonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        if (Config.needsReset()) {
            LOGGER.info("Config version outdated, resetting to defaults...");
            Config.resetToDefaults();
        }
    }

    private boolean isDevelopmentEnvironment(){
        return "development".equals(System.getenv("ENV"));
    }

    private String alertKey;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        SafeMode.setServer(event.getServer());
        try {
            String url;
            if (isDevelopmentEnvironment()) {
                url = "ws://127.0.0.1:8006/integration";
            } else {
                url = "wss://alerts.isaiahcreati.com/integration";
            }

            alertKey = Config.ALERT_KEY.get();
            socket = IO.socket(url);

            socket.on(Socket.EVENT_CONNECT, args -> {
                reconnectAttempts = 0;
                LOGGER.info("Connected to SocketIO");
                if (alertKey != null && !alertKey.isEmpty()) {
                    socket.emit("join", alertKey);
                }
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

                    if (SafeMode.isActive()) {
                        int remaining = SafeMode.getRemainingSeconds();
                        LOGGER.info("Safe mode active ({}s remaining) — blocked interaction", remaining);
                        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
                            Chat.SendMessage(p, "\u00a7d\u00a7l\u2696 Safe Mode \u00a7r\u00a77is active (\u00a7d" + remaining + "s\u00a77) — redeem blocked");
                        }
                        return;
                    }

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

                                Chat.SendAlert(player, "&b" + payload.metadata.redeemerName + "&7 gave you &bx" + itemDetails.amount + " " + item.getName(item.getDefaultInstance()).getString());
                                String giveIcon = ToastIconHelper.getIconForAction("GIVE", itemDetails.itemId.contains(":") ? itemDetails.itemId : "minecraft:" + itemDetails.itemId);
                                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("GIVE", "", payload.metadata.redeemerName, itemDetails.amount + "x " + item.getName(item.getDefaultInstance()).getString(), 0, giveIcon));
                                break;
                            case SPAWN:
                                if (!(payload.details instanceof SpawnDetails spawnDetails)) break;
                                Mobs.spawnMobNearPlayer(player, spawnDetails.mobId, spawnDetails.amount, payload.metadata.redeemerName);
                                EntityType mob = Mobs.getMobByName(spawnDetails.mobId);
                                Chat.SendAlert(player, "&b" + payload.metadata.redeemerName + "&7 spawned &bx" + spawnDetails.amount + " " + mob.getDescription().getString());
                                String spawnIcon = ToastIconHelper.getIconForAction("SPAWN", spawnDetails.mobId.contains(":") ? spawnDetails.mobId : "minecraft:" + spawnDetails.mobId);
                                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("SPAWN", "", payload.metadata.redeemerName, spawnDetails.amount + "x " + mob.getDescription().getString(), 0, spawnIcon));
                                break;
                            case EFFECT:
                                if (!(payload.details instanceof EffectDetails effectDetails)) break;

                                Taunts.applyPotionEffect(player, effectDetails.potionId, effectDetails.duration, effectDetails.amplifier);
                                Holder<MobEffect> effect = Utils.getPotionEffect(effectDetails.potionId);
                                Chat.SendAlert(player, "&b" + payload.metadata.redeemerName + "&7 splashed you with &b" + effect.value().getDescriptionId() + " " + Chat.NumberToRoman(effectDetails.amplifier + 1) + "!");
                                String effectIcon = ToastIconHelper.getIconForAction("EFFECT", "");
                                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("EFFECT", "", payload.metadata.redeemerName, effect.value().getDescriptionId(), 0, effectIcon));
                                break;
                            case TAUNT:
                                if (!(payload.details instanceof TauntDetails tauntDetails)) break;
                                Taunt taunt = taunts.getTauntById(tauntDetails.tauntId);
                                String tauntId = tauntDetails.tauntId;
                                String tauntIcon = ToastIconHelper.getIconForTaunt(tauntId);
                                if (Config.QUEUE_ENABLED.get()) {
                                    QueueManager.enqueue(player, tauntId, payload.metadata.redeemerName, 15, tauntDetails.mobType);
                                } else {
                                    boolean dispatched = TauntDispatcher.dispatchTaunt(player, tauntId, 15, tauntDetails.mobType);
                                    if (taunt != null && dispatched) {
                                        Chat.SendAlert(player, "&b" + payload.metadata.redeemerName + "&7 taunted you with &b" + taunt.getDisplayName());
                                        PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_INSTANT", tauntId, payload.metadata.redeemerName, "", 0, tauntIcon));
                                    }
                                }
                        }
                    }

                } catch (JsonSyntaxException e) {
                    LOGGER.error("Failed process interaction: " + e);
                    e.printStackTrace();
                }
            });

            if (Config.AUTO_CONNECT.get() && EventHandler.isConfigSetup()) {
                socket.connect();
                LOGGER.info("Auto-connecting to SocketIO...");
            }
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
        if (!Config.ONBOARDED.get()) {
            if (event.getEntity() instanceof ServerPlayer player) {
                ItemStack book = OnboardingBook.create();
                if (!player.getInventory().add(book)) {
                    player.spawnAtLocation((ServerLevel) player.level(), book);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack heldItem = event.getItemStack();
            if (OnboardingBook.isOnboardingBook(heldItem)) {
                PacketHandler.sendOnboardingScreen(player);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        message = message.replace('&', '\u00a7');
        var sender = event.getPlayer();
        if (sender instanceof ServerPlayer player) {
            long currentTick = player.level().getServer().getTickCount();
            Taunts.RenameState rename = Taunts.getActiveRename(player.getUUID(), currentTick);
            if (rename != null) {
                Component originalName = Component.literal(sender.getName().getString())
                        .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(0xAAAAAA)).withItalic(true));
                Component renamed = Component.literal(rename.name()).append(Component.literal(" (").append(originalName).append(Component.literal(")")));
                Component body = Component.literal(message);
                Component composed = Component.literal("<").append(renamed).append(Component.literal("> ")).append(body);
                event.setMessage(composed);
                return;
            }
        }
        event.setMessage(Component.literal(message));
    }

    @SubscribeEvent
    public void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (Taunts.isLuckyBlock(event.getPos())) {
            event.setCanceled(true);
            if (event.getLevel() instanceof ServerLevel level) {
                level.setBlock(event.getPos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
            }
            Taunts.onLuckyBlockBroken(player, event.getPos());
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        net.minecraft.world.level.block.state.BlockState placed = event.getPlacedBlock();
        if (placed == null) return;
        var heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) heldItem = player.getOffhandItem();
        if (!Taunts.isLuckyBlockItem(heldItem)) return;
        if (!placed.is(net.minecraft.world.level.block.Blocks.SPONGE)) return;
        Taunts.registerPlacedLuckyBlock(level, event.getPos());
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Taunts.clearHotPotatoOnDeath(player);
    }

}