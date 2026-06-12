package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;

import com.isaiahcreati.creatibotintegration.helpers.TauntDispatcher;
import com.isaiahcreati.creatibotintegration.integration.minigame.Minigame;
import com.isaiahcreati.creatibotintegration.network.ClientboundActivityNotificationPacket;
import com.isaiahcreati.creatibotintegration.network.ClientboundQueueUpdatePacket;
import com.isaiahcreati.creatibotintegration.network.ClientboundTauntEffectPacket;
import com.isaiahcreati.creatibotintegration.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class QueueManager {

    private static final LinkedList<QueuedTaunt> minigameQueue = new LinkedList<>();
    private static final LinkedList<QueuedTaunt> visualEffectQueue = new LinkedList<>();
    private static final LinkedList<QueuedTaunt> pendingTaunts = new LinkedList<>();

    private static boolean minigameActive = false;
    private static String activeMinigameId = null;
    private static String activeMinigameRedeemer = null;

    private static String activeVisualEffectId = null;
    private static String activeVisualEffectRedeemer = null;
    private static long activeVisualEffectExpiryTick = 0;
    private static int activeVisualEffectDurationSeconds = 0;

    private static boolean visualEffectsPaused = false;
    private static String pausedVisualEffectId = null;
    private static String pausedVisualEffectRedeemer = null;
    private static long pausedVisualEffectRemainingTicks = 0;

    private static int staggerDelayTicks = 0;
    private static boolean staggerReleasing = false;

    private static final Set<String> VISUAL_EFFECT_IDS = Set.of(
            "blur", "inverted_colors", "black_and_white", "lsd", "crt",
            "pumpkin_view", "dvd"
    );

    private static final Set<String> MINIGAME_IDS = Set.of(
            "parkour", "tntrun", "dropper"
    );

    public static boolean isVisualEffect(String tauntId) {
        return VISUAL_EFFECT_IDS.contains(tauntId);
    }

    public static boolean isMinigame(String tauntId) {
        return MINIGAME_IDS.contains(tauntId);
    }

    public static boolean isMinigameActive() {
        return minigameActive;
    }

    public static void enqueue(ServerPlayer player, String tauntId, String redeemerName) {
        enqueue(player, tauntId, redeemerName, 15);
    }

    public static void enqueue(ServerPlayer player, String tauntId, String redeemerName, int durationSeconds) {
        if (!Config.QUEUE_ENABLED.get()) {
            TauntDispatcher.dispatchTaunt(player, tauntId, durationSeconds);
            PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_INSTANT", tauntId, redeemerName, "", 0));
            return;
        }

        long currentTick = player.level().getServer().getTickCount();

        if (isMinigame(tauntId)) {
            if (!minigameActive) {
                minigameActive = true;
                activeMinigameId = tauntId;
                activeMinigameRedeemer = redeemerName;
                boolean dispatched = dispatchMinigame(player, tauntId, redeemerName);
                if (!dispatched) {
                    minigameActive = false;
                    activeMinigameId = null;
                    activeMinigameRedeemer = null;
                } else {
                    pauseVisualEffects(player);
                    PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("MINIGAME_ACTIVATED", tauntId, redeemerName, "", 0));
                }
            } else {
                QueuedTaunt entry = new QueuedTaunt(tauntId, redeemerName, durationSeconds, currentTick);
                minigameQueue.add(entry);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("MINIGAME_QUEUED", tauntId, redeemerName, "", minigameQueue.size()));
            }
        } else if (isVisualEffect(tauntId)) {
            if (minigameActive) {
                QueuedTaunt entry = new QueuedTaunt(tauntId, redeemerName, durationSeconds, currentTick);
                visualEffectQueue.add(entry);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("VISUAL_EFFECT_QUEUED", tauntId, redeemerName, "", visualEffectQueue.size()));
            } else if (activeVisualEffectId == null && !visualEffectsPaused) {
                activateVisualEffect(player, tauntId, redeemerName, durationSeconds);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("VISUAL_EFFECT_ACTIVATED", tauntId, redeemerName, "", 0));
            } else if (activeVisualEffectId != null && activeVisualEffectId.equals(tauntId)) {
                extendVisualEffect(player, redeemerName, durationSeconds);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_EXTENDED", tauntId, redeemerName, "", 0));
            } else {
                QueuedTaunt entry = new QueuedTaunt(tauntId, redeemerName, durationSeconds, currentTick);
                visualEffectQueue.add(entry);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("VISUAL_EFFECT_QUEUED", tauntId, redeemerName, "", visualEffectQueue.size()));
            }
        } else {
            if (minigameActive) {
                QueuedTaunt entry = new QueuedTaunt(tauntId, redeemerName, durationSeconds, currentTick);
                pendingTaunts.add(entry);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_QUEUED", tauntId, redeemerName, "", pendingTaunts.size()));
            } else {
                TauntDispatcher.dispatchTaunt(player, tauntId, durationSeconds);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_INSTANT", tauntId, redeemerName, "", 0));
            }
        }

        broadcastQueueUpdate(player);
    }

    private static void activateVisualEffect(ServerPlayer player, String effectId, String redeemerName, int durationSeconds) {
        activeVisualEffectId = effectId;
        activeVisualEffectRedeemer = redeemerName;
        long currentTick = player.level().getServer().getTickCount();
        activeVisualEffectExpiryTick = currentTick + (durationSeconds * 20L);
        activeVisualEffectDurationSeconds = durationSeconds;
        PacketHandler.sendToPlayer(player, new ClientboundTauntEffectPacket(effectId, durationSeconds));
    }

    private static void extendVisualEffect(ServerPlayer player, String redeemerName, int additionalSeconds) {
        activeVisualEffectExpiryTick += (additionalSeconds * 20L);
        long currentTick = player.level().getServer().getTickCount();
        int remainingSeconds = (int) Math.max(0, (activeVisualEffectExpiryTick - currentTick) / 20);
        PacketHandler.sendToPlayer(player, new ClientboundTauntEffectPacket(activeVisualEffectId, remainingSeconds));
    }

    private static void pauseVisualEffects(ServerPlayer player) {
        if (activeVisualEffectId == null) return;

        long currentTick = player.level().getServer().getTickCount();
        pausedVisualEffectRemainingTicks = Math.max(0, activeVisualEffectExpiryTick - currentTick);
        pausedVisualEffectId = activeVisualEffectId;
        pausedVisualEffectRedeemer = activeVisualEffectRedeemer;
        visualEffectsPaused = true;

        PacketHandler.sendToPlayer(player, new ClientboundTauntEffectPacket("pause_effects", 0));

        activeVisualEffectId = null;
        activeVisualEffectRedeemer = null;
        activeVisualEffectExpiryTick = 0;
    }

    private static void resumeVisualEffects(ServerPlayer player) {
        if (!visualEffectsPaused) {
            processNextVisualEffect(player);
            return;
        }

        visualEffectsPaused = false;

        if (pausedVisualEffectRemainingTicks <= 0 || pausedVisualEffectId == null) {
            pausedVisualEffectId = null;
            pausedVisualEffectRedeemer = null;
            pausedVisualEffectRemainingTicks = 0;
            processNextVisualEffect(player);
            return;
        }

        int remainingSeconds = (int) (pausedVisualEffectRemainingTicks / 20);
        if (remainingSeconds <= 0) {
            pausedVisualEffectId = null;
            pausedVisualEffectRedeemer = null;
            pausedVisualEffectRemainingTicks = 0;
            processNextVisualEffect(player);
            return;
        }

        activeVisualEffectId = pausedVisualEffectId;
        activeVisualEffectRedeemer = pausedVisualEffectRedeemer;
        long currentTick = player.level().getServer().getTickCount();
        activeVisualEffectExpiryTick = currentTick + pausedVisualEffectRemainingTicks;
        activeVisualEffectDurationSeconds = remainingSeconds;

        PacketHandler.sendToPlayer(player, new ClientboundTauntEffectPacket("resume_effects", remainingSeconds));
        pausedVisualEffectId = null;
        pausedVisualEffectRedeemer = null;
        pausedVisualEffectRemainingTicks = 0;
    }

    public static void onMinigameEnd(ServerPlayer player) {
        if (!Config.QUEUE_ENABLED.get()) return;

        minigameActive = false;
        activeMinigameId = null;
        activeMinigameRedeemer = null;

        if (!pendingTaunts.isEmpty()) {
            staggerReleasing = true;
            staggerDelayTicks = Config.STAGGER_DELAY_TICKS.get();
        } else {
            resumeVisualEffects(player);
            processNextMinigame(player);
        }

        broadcastQueueUpdate(player);
    }

    private static int broadcastCooldown = 0;
    private static final int BROADCAST_INTERVAL_TICKS = 20;

    public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        long currentTick = event.getServer().getTickCount();

        if (activeVisualEffectId != null && !visualEffectsPaused) {
            if (currentTick >= activeVisualEffectExpiryTick) {
                activeVisualEffectId = null;
                activeVisualEffectRedeemer = null;
                activeVisualEffectExpiryTick = 0;

                ServerPlayer player = event.getServer().getPlayerList().getPlayers().isEmpty() ? null : event.getServer().getPlayerList().getPlayers().get(0);
                if (player != null) {
                    processNextVisualEffect(player);
                }
                broadcastQueueUpdate();
            }
        }

        if (staggerReleasing) {
            staggerDelayTicks--;
            if (staggerDelayTicks <= 0) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayers().isEmpty() ? null : event.getServer().getPlayerList().getPlayers().get(0);
                if (player != null) {
                    releaseNextPendingTaunt(player);
                }
            }
        }

        broadcastCooldown--;
        if (broadcastCooldown <= 0) {
            broadcastCooldown = BROADCAST_INTERVAL_TICKS;
            if (activeVisualEffectId != null || !minigameQueue.isEmpty() || !visualEffectQueue.isEmpty()
                    || minigameActive || !pendingTaunts.isEmpty() || visualEffectsPaused) {
                broadcastQueueUpdate();
            }
        }
    }

    private static void releaseNextPendingTaunt(ServerPlayer player) {
        if (pendingTaunts.isEmpty()) {
            staggerReleasing = false;
            resumeVisualEffects(player);
            processNextMinigame(player);
            broadcastQueueUpdate(player);
            return;
        }

        QueuedTaunt taunt = pendingTaunts.pollFirst();
        TauntDispatcher.dispatchTaunt(player, taunt.getTauntId(), taunt.getDurationSeconds());
        PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_ACTIVATED", taunt.getTauntId(), taunt.getRedeemerName(), "", 0));

        staggerDelayTicks = Config.STAGGER_DELAY_TICKS.get();
        broadcastQueueUpdate(player);
    }

    private static void processNextVisualEffect(ServerPlayer player) {
        if (!visualEffectQueue.isEmpty()) {
            QueuedTaunt next = visualEffectQueue.pollFirst();
            activateVisualEffect(player, next.getTauntId(), next.getRedeemerName(), next.getDurationSeconds());
            PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("VISUAL_EFFECT_ACTIVATED", next.getTauntId(), next.getRedeemerName(), "", 0));
        }
    }

    private static void processNextMinigame(ServerPlayer player) {
        if (!minigameActive && !minigameQueue.isEmpty()) {
            QueuedTaunt next = minigameQueue.pollFirst();
            minigameActive = true;
            activeMinigameId = next.getTauntId();
            activeMinigameRedeemer = next.getRedeemerName();
            boolean dispatched = dispatchMinigame(player, next.getTauntId(), next.getRedeemerName());
            if (!dispatched) {
                minigameActive = false;
                activeMinigameId = null;
                activeMinigameRedeemer = null;
            } else {
                pauseVisualEffects(player);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("MINIGAME_ACTIVATED", next.getTauntId(), next.getRedeemerName(), "", 0));
            }
        }
    }

    private static boolean dispatchMinigame(ServerPlayer player, String tauntId, String redeemerName) {
        Minigame game = switch (tauntId) {
            case "parkour" -> CreatiIntegration.getParkourMinigame();
            case "tntrun" -> CreatiIntegration.getTntRunMinigame();
            case "dropper" -> CreatiIntegration.getDropperMinigame();
            default -> null;
        };
        if (game == null) return false;
        if (!game.isEnabled()) return false;
        if (game.isInMinigame(player)) return false;
        game.enterPlayer(player, redeemerName);
        return true;
    }

    public static void broadcastQueueUpdate() {
        if (!Config.QUEUE_ENABLED.get()) return;

        int veRemaining = 0;
        if (activeVisualEffectId != null && !visualEffectsPaused) {
            long currentTick = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null
                    ? net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getTickCount() : 0;
            veRemaining = (int) Math.max(0, (activeVisualEffectExpiryTick - currentTick) / 20);
        }

        ClientboundQueueUpdatePacket packet = new ClientboundQueueUpdatePacket(
                buildQueueEntries(minigameQueue),
                buildQueueEntries(visualEffectQueue),
                activeMinigameId != null ? activeMinigameId : "",
                activeMinigameRedeemer != null ? activeMinigameRedeemer : "",
                activeVisualEffectId != null ? activeVisualEffectId : "",
                activeVisualEffectRedeemer != null ? activeVisualEffectRedeemer : "",
                veRemaining,
                activeVisualEffectDurationSeconds,
                (int)(pausedVisualEffectRemainingTicks / 20),
                pendingTaunts.size()
        );

        PacketHandler.sendToAll(packet);
    }

    public static void broadcastQueueUpdate(ServerPlayer player) {
        broadcastQueueUpdate();
    }

    private static List<ClientboundQueueUpdatePacket.QueueEntry> buildQueueEntries(LinkedList<QueuedTaunt> queue) {
        List<ClientboundQueueUpdatePacket.QueueEntry> entries = new java.util.ArrayList<>();
        for (QueuedTaunt t : queue) {
            entries.add(new ClientboundQueueUpdatePacket.QueueEntry(t.getTauntId(), t.getRedeemerName(), t.getDurationSeconds()));
        }
        return entries;
    }

    public static String getActiveMinigameId() {
        return activeMinigameId;
    }

    public static String getActiveMinigameRedeemer() {
        return activeMinigameRedeemer;
    }

    public static String getActiveVisualEffectId() {
        return activeVisualEffectId;
    }

    public static int getMinigameQueueSize() {
        return minigameQueue.size();
    }

    public static int getVisualEffectQueueSize() {
        return visualEffectQueue.size();
    }

    public static int getPendingTauntsSize() {
        return pendingTaunts.size();
    }

    private static final Taunts tauntRegistry = new Taunts();

    private static String getTauntDisplayName(String tauntId) {
        Taunt taunt = tauntRegistry.getTauntById(tauntId);
        if (taunt != null) return taunt.getDisplayName();
        return tauntId;
    }
}