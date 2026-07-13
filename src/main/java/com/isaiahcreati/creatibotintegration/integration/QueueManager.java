package com.isaiahcreati.creatibotintegration.integration;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;

import com.isaiahcreati.creatibotintegration.helpers.Buffs;
import com.isaiahcreati.creatibotintegration.helpers.SafeMode;
import com.isaiahcreati.creatibotintegration.helpers.TauntDispatcher;
import com.isaiahcreati.creatibotintegration.helpers.ToastIconHelper;
import com.isaiahcreati.creatibotintegration.integration.minigame.Minigame;
import com.isaiahcreati.creatibotintegration.network.ClientboundActivityNotificationPacket;
import com.isaiahcreati.creatibotintegration.network.ClientboundQueueUpdatePacket;
import com.isaiahcreati.creatibotintegration.network.ClientboundTauntEffectPacket;
import com.isaiahcreati.creatibotintegration.network.PacketHandler;
import net.minecraft.server.MinecraftServer;
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
    private static boolean queueWasEnabled = false;
    private static int lastSafeModeRemainingSeconds = 0;

    private static final Set<String> VISUAL_EFFECT_IDS = Set.of(
            "blur", "inverted_colors", "black_and_white", "lsd", "crt",
            "pumpkin_view", "dvd", "drunk", "vignette_heartbeat",
            "pixelate", "mirror", "fisheye",
            "fov_quake", "fov_zoom", "upside_down", "rolling_camera", "camera_tilt"
    );

    private static final Set<String> MINIGAME_IDS = Set.of(
            "parkour", "tntrun", "dropper", "sumo"
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

    public static void resetForServerStart() {
        clearQueueState(true);
        queueWasEnabled = false;
        broadcastCooldown = 0;
        lastSafeModeRemainingSeconds = 0;
    }

    public static void resetForServerStop() {
        clearQueueState(true);
        broadcastQueueUpdate();
        queueWasEnabled = false;
    }

    private static void clearQueueState(boolean clearActiveBuffs) {
        minigameQueue.clear();
        visualEffectQueue.clear();
        pendingTaunts.clear();

        minigameActive = false;
        activeMinigameId = null;
        activeMinigameRedeemer = null;

        activeVisualEffectId = null;
        activeVisualEffectRedeemer = null;
        activeVisualEffectExpiryTick = 0;
        activeVisualEffectDurationSeconds = 0;

        visualEffectsPaused = false;
        pausedVisualEffectId = null;
        pausedVisualEffectRedeemer = null;
        pausedVisualEffectRemainingTicks = 0;

        staggerDelayTicks = 0;
        staggerReleasing = false;

        if (clearActiveBuffs) {
            Buffs.resetState();
        } else {
            Buffs.clearPendingBuffs();
        }
    }

    private static void clearWaitingQueues() {
        minigameQueue.clear();
        visualEffectQueue.clear();
        pendingTaunts.clear();
        Buffs.clearPendingBuffs();
        staggerDelayTicks = 0;
        staggerReleasing = false;
    }

    private static boolean hasQueueState() {
        return minigameActive
                || activeVisualEffectId != null
                || visualEffectsPaused
                || !minigameQueue.isEmpty()
                || !visualEffectQueue.isEmpty()
                || !pendingTaunts.isEmpty()
                || Buffs.getPendingBuffsSize() > 0;
    }

    public static void enqueue(ServerPlayer player, String tauntId, String redeemerName) {
        enqueue(player, tauntId, redeemerName, 15, null);
    }

    public static void enqueue(ServerPlayer player, String tauntId, String redeemerName, int durationSeconds) {
        enqueue(player, tauntId, redeemerName, durationSeconds, null);
    }

    public static void enqueue(ServerPlayer player, String tauntId, String redeemerName, int durationSeconds, String mobType) {
        if (!Config.QUEUE_ENABLED.get()) {
            if (TauntDispatcher.dispatchTaunt(player, tauntId, durationSeconds, mobType)) {
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_INSTANT", tauntId, redeemerName, "", 0, ToastIconHelper.getIconForTaunt(tauntId)));
            }
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
                    PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket(
                            "MINIGAME_SKIPPED", tauntId, redeemerName, "Unavailable", 0,
                            ToastIconHelper.getIconForTaunt(tauntId)));
                } else {
                    pauseVisualEffects(player);
                    PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("MINIGAME_ACTIVATED", tauntId, redeemerName, "", 0, ToastIconHelper.getIconForTaunt(tauntId)));
                }
            } else {
                QueuedTaunt entry = new QueuedTaunt(tauntId, redeemerName, durationSeconds, currentTick, mobType);
                minigameQueue.add(entry);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("MINIGAME_QUEUED", tauntId, redeemerName, "", minigameQueue.size(), ToastIconHelper.getIconForTaunt(tauntId)));
            }
        } else if (isVisualEffect(tauntId)) {
            if (minigameActive) {
                QueuedTaunt entry = new QueuedTaunt(tauntId, redeemerName, durationSeconds, currentTick, mobType);
                visualEffectQueue.add(entry);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("VISUAL_EFFECT_QUEUED", tauntId, redeemerName, "", visualEffectQueue.size(), ToastIconHelper.getIconForTaunt(tauntId)));
            } else if (activeVisualEffectId == null && !visualEffectsPaused) {
                activateVisualEffect(player, tauntId, redeemerName, durationSeconds);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("VISUAL_EFFECT_ACTIVATED", tauntId, redeemerName, "", 0, ToastIconHelper.getIconForTaunt(tauntId)));
            } else if (activeVisualEffectId != null && activeVisualEffectId.equals(tauntId)) {
                extendVisualEffect(player, redeemerName, durationSeconds);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_EXTENDED", tauntId, redeemerName, "", 0, ToastIconHelper.getIconForTaunt(tauntId)));
            } else {
                QueuedTaunt entry = new QueuedTaunt(tauntId, redeemerName, durationSeconds, currentTick, mobType);
                visualEffectQueue.add(entry);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("VISUAL_EFFECT_QUEUED", tauntId, redeemerName, "", visualEffectQueue.size(), ToastIconHelper.getIconForTaunt(tauntId)));
            }
        } else {
            if (minigameActive) {
                QueuedTaunt entry = new QueuedTaunt(tauntId, redeemerName, durationSeconds, currentTick, mobType);
                pendingTaunts.add(entry);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_QUEUED", tauntId, redeemerName, "", pendingTaunts.size(), ToastIconHelper.getIconForTaunt(tauntId)));
            } else {
                if (TauntDispatcher.dispatchTaunt(player, tauntId, durationSeconds, mobType)) {
                    PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_INSTANT", tauntId, redeemerName, "", 0, ToastIconHelper.getIconForTaunt(tauntId)));
                }
            }
        }

        broadcastQueueUpdate();
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
        activeVisualEffectDurationSeconds = remainingSeconds;
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
        minigameActive = false;
        activeMinigameId = null;
        activeMinigameRedeemer = null;

        if (!Config.QUEUE_ENABLED.get()) {
            resumeVisualEffects(player);
            broadcastQueueUpdate();
            return;
        }

        Buffs.releasePendingBuffs();

        if (!pendingTaunts.isEmpty()) {
            staggerReleasing = true;
            staggerDelayTicks = Config.STAGGER_DELAY_TICKS.get();
        } else {
            if (!processNextMinigame(player)) {
                resumeVisualEffects(player);
            }
        }

        broadcastQueueUpdate();
    }

    private static int broadcastCooldown = 0;
    private static final int BROADCAST_INTERVAL_TICKS = 20;

    public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        long currentTick = event.getServer().getTickCount();
        boolean queueEnabled = Config.QUEUE_ENABLED.get();

        if (queueEnabled && !queueWasEnabled) {
            queueWasEnabled = true;
            broadcastQueueUpdate();
        } else if (!queueEnabled && queueWasEnabled) {
            clearWaitingQueues();
            queueWasEnabled = false;
            broadcastQueueUpdate();
        }

        if (activeVisualEffectId != null && !visualEffectsPaused) {
            if (currentTick >= activeVisualEffectExpiryTick) {
                activeVisualEffectId = null;
                activeVisualEffectRedeemer = null;
                activeVisualEffectExpiryTick = 0;
                activeVisualEffectDurationSeconds = 0;

                ServerPlayer player = getPrimaryPlayer(event.getServer());
                if (player != null) {
                    processNextVisualEffect(player);
                }
                broadcastQueueUpdate();
            }
        }

        if (queueEnabled && staggerReleasing) {
            staggerDelayTicks--;
            if (staggerDelayTicks <= 0) {
                ServerPlayer player = getPrimaryPlayer(event.getServer());
                if (player != null) {
                    releaseNextPendingTaunt(player);
                }
            }
        }

        broadcastCooldown--;
        if (broadcastCooldown <= 0) {
            broadcastCooldown = BROADCAST_INTERVAL_TICKS;
            int safeModeRemaining = SafeMode.isActive() ? SafeMode.getRemainingSeconds() : 0;
            if (hasQueueState() || safeModeRemaining > 0 || lastSafeModeRemainingSeconds > 0) {
                broadcastQueueUpdate();
            }
            lastSafeModeRemainingSeconds = safeModeRemaining;
        }
    }

    private static ServerPlayer getPrimaryPlayer(MinecraftServer server) {
        return server.getPlayerList().getPlayers().isEmpty()
                ? null
                : server.getPlayerList().getPlayers().getFirst();
    }

    private static void releaseNextPendingTaunt(ServerPlayer player) {
        if (pendingTaunts.isEmpty()) {
            staggerReleasing = false;
            Buffs.releasePendingBuffs();
            if (!processNextMinigame(player)) {
                resumeVisualEffects(player);
            }
            broadcastQueueUpdate();
            return;
        }

        QueuedTaunt taunt = pendingTaunts.pollFirst();
        if (TauntDispatcher.dispatchTaunt(player, taunt.getTauntId(), taunt.getDurationSeconds(), taunt.getMobType())) {
            PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("TAUNT_ACTIVATED", taunt.getTauntId(), taunt.getRedeemerName(), "", 0, ToastIconHelper.getIconForTaunt(taunt.getTauntId())));
        }

        staggerDelayTicks = Config.STAGGER_DELAY_TICKS.get();
        broadcastQueueUpdate();
    }

    private static void processNextVisualEffect(ServerPlayer player) {
        if (!visualEffectQueue.isEmpty()) {
            QueuedTaunt next = visualEffectQueue.pollFirst();
            activateVisualEffect(player, next.getTauntId(), next.getRedeemerName(), next.getDurationSeconds());
            PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("VISUAL_EFFECT_ACTIVATED", next.getTauntId(), next.getRedeemerName(), "", 0, ToastIconHelper.getIconForTaunt(next.getTauntId())));
        }
    }

    private static boolean processNextMinigame(ServerPlayer player) {
        while (!minigameActive && !minigameQueue.isEmpty()) {
            QueuedTaunt next = minigameQueue.pollFirst();
            minigameActive = true;
            activeMinigameId = next.getTauntId();
            activeMinigameRedeemer = next.getRedeemerName();
            boolean dispatched = dispatchMinigame(player, next.getTauntId(), next.getRedeemerName());
            if (!dispatched) {
                minigameActive = false;
                activeMinigameId = null;
                activeMinigameRedeemer = null;
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket(
                        "MINIGAME_SKIPPED", next.getTauntId(), next.getRedeemerName(), "Unavailable", 0,
                        ToastIconHelper.getIconForTaunt(next.getTauntId())));
            } else {
                pauseVisualEffects(player);
                PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("MINIGAME_ACTIVATED", next.getTauntId(), next.getRedeemerName(), "", 0, ToastIconHelper.getIconForTaunt(next.getTauntId())));
                return true;
            }
        }
        return minigameActive;
    }

    private static boolean dispatchMinigame(ServerPlayer player, String tauntId, String redeemerName) {
        Minigame game = switch (tauntId) {
            case "parkour" -> CreatiIntegration.getParkourMinigame();
            case "tntrun" -> CreatiIntegration.getTntRunMinigame();
            case "dropper" -> CreatiIntegration.getDropperMinigame();
            case "sumo" -> CreatiIntegration.getSumoMinigame();
            default -> null;
        };
        if (game == null) return false;
        if (!game.isEnabled()) return false;
        if (game.isInMinigame(player)) return false;
        game.enterPlayer(player, redeemerName);
        return game.isInMinigame(player);
    }

    public static void broadcastQueueUpdate() {
        PacketHandler.sendToAll(createQueueUpdatePacket());
    }

    public static void sendQueueUpdate(ServerPlayer player) {
        PacketHandler.sendToPlayer(player, createQueueUpdatePacket());
    }

    public static ClientboundQueueUpdatePacket createQueueUpdatePacket() {

        int veRemaining = 0;
        if (activeVisualEffectId != null && !visualEffectsPaused) {
            long currentTick = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer() != null
                    ? net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getTickCount() : 0;
            veRemaining = (int) Math.max(0, (activeVisualEffectExpiryTick - currentTick) / 20);
        }

        return new ClientboundQueueUpdatePacket(
                buildQueueEntries(minigameQueue),
                buildQueueEntries(visualEffectQueue),
                buildQueueEntries(pendingTaunts),
                buildPendingBuffEntries(),
                activeMinigameId != null ? activeMinigameId : "",
                activeMinigameRedeemer != null ? activeMinigameRedeemer : "",
                activeVisualEffectId != null ? activeVisualEffectId : "",
                activeVisualEffectRedeemer != null ? activeVisualEffectRedeemer : "",
                pausedVisualEffectId != null ? pausedVisualEffectId : "",
                pausedVisualEffectRedeemer != null ? pausedVisualEffectRedeemer : "",
                veRemaining,
                activeVisualEffectDurationSeconds,
                (int)(pausedVisualEffectRemainingTicks / 20),
                SafeMode.isActive() ? SafeMode.getRemainingSeconds() : 0
        );
    }

    private static List<ClientboundQueueUpdatePacket.QueueEntry> buildQueueEntries(LinkedList<QueuedTaunt> queue) {
        List<ClientboundQueueUpdatePacket.QueueEntry> entries = new java.util.ArrayList<>();
        for (QueuedTaunt t : queue) {
            entries.add(new ClientboundQueueUpdatePacket.QueueEntry(
                    t.getTauntId(), getTauntDisplayName(t.getTauntId()), t.getRedeemerName(), t.getDurationSeconds()));
        }
        return entries;
    }

    private static List<ClientboundQueueUpdatePacket.QueueEntry> buildPendingBuffEntries() {
        List<ClientboundQueueUpdatePacket.QueueEntry> entries = new java.util.ArrayList<>();
        for (Buffs.PendingBuff buff : Buffs.getPendingBuffsSnapshot()) {
            entries.add(new ClientboundQueueUpdatePacket.QueueEntry(
                    buff.buffId(), Buffs.getDisplayName(buff.buffId()), buff.redeemerName(), buff.durationSeconds()));
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
