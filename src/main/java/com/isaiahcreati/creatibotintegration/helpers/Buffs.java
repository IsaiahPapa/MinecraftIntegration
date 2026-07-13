package com.isaiahcreati.creatibotintegration.helpers;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.integration.QueueManager;
import com.isaiahcreati.creatibotintegration.network.ClientboundActivityNotificationPacket;
import com.isaiahcreati.creatibotintegration.network.PacketHandler;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Buffs {
    private static final Logger LOGGER = LoggerFactory.getLogger("Buffs");

    public record ActiveBuff(String buffId, long expiryTick, int durationSeconds) {}

    private static final Map<UUID, List<ActiveBuff>> activeBuffs = new ConcurrentHashMap<>();

    public record PendingBuff(UUID playerUuid, String buffId, int durationSeconds, String redeemerName) {}
    private static final LinkedList<PendingBuff> pendingBuffs = new LinkedList<>();

    private static final String FROSTBITE_TAG = "creatibotintegration.frostbite";

    public static void handleBuffActivation(ServerPlayer player, String buffId, int duration, String redeemerName) {
        if (Config.QUEUE_ENABLED.get() && QueueManager.isMinigameActive()) {
            pendingBuffs.add(new PendingBuff(player.getUUID(), buffId, duration, redeemerName));
            Chat.SendAlert(player, "&b" + redeemerName + "&7 activated &b" + getDisplayName(buffId) + "&7 — queued (minigame active)");
            PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket(
                    "BUFF_QUEUED", buffId, redeemerName, "", pendingBuffs.size(), "item:minecraft:beacon"));
            QueueManager.broadcastQueueUpdate();
            return;
        }
        activate(player, buffId, duration, redeemerName);
    }

    public static void activate(ServerPlayer player, String buffId, int duration, String redeemerName) {
        long expiryTick = player.level().getServer().getTickCount() + (duration * 20L);
        activeBuffs.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new ActiveBuff(buffId, expiryTick, duration));

        String name = getDisplayName(buffId);
        Chat.SendAlert(player, "&b" + redeemerName + "&7 activated &b" + name + "&7 for &b" + duration + "s");
        PacketHandler.sendToPlayer(player, new ClientboundActivityNotificationPacket("BUFF_ACTIVATED", buffId, redeemerName, name + " (" + duration + "s)", 0, "item:minecraft:beacon"));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        LOGGER.info("Activated buff {} for {} ({}s)", buffId, player.getName().getString(), duration);
    }

    public static void releasePendingBuffs() {
        if (pendingBuffs.isEmpty()) return;
        if (QueueManager.isMinigameActive()) return;

        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        var it = pendingBuffs.iterator();
        while (it.hasNext()) {
            PendingBuff pending = it.next();
            it.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(pending.playerUuid());
            if (player != null) {
                activate(player, pending.buffId(), pending.durationSeconds(), pending.redeemerName());
            }
        }
        QueueManager.broadcastQueueUpdate();
    }

    public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (activeBuffs.isEmpty()) return;
        long currentTick = event.getServer().getTickCount();

        var it = activeBuffs.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            List<ActiveBuff> buffs = entry.getValue();
            buffs.removeIf(buff -> {
                if (currentTick >= buff.expiryTick()) {
                    ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                    if (player != null) {
                        Chat.SendAlert(player, "&7Buff &b" + getDisplayName(buff.buffId()) + "&7 expired");
                    }
                    LOGGER.info("Buff {} expired for {}", buff.buffId(), entry.getKey());
                    return true;
                }
                return false;
            });
            if (buffs.isEmpty()) it.remove();
        }
    }

    public static List<ActiveBuff> getActiveBuffs(UUID playerUuid) {
        return activeBuffs.getOrDefault(playerUuid, Collections.emptyList());
    }

    public static boolean hasActiveBuff(UUID playerUuid, String buffId) {
        return activeBuffs.getOrDefault(playerUuid, Collections.emptyList()).stream()
                .anyMatch(buff -> buff.buffId().equals(buffId));
    }

    public static void applyBuffsToSpawnedMob(ServerPlayer player, Mob mob) {
        List<ActiveBuff> buffs = getActiveBuffs(player.getUUID());
        if (buffs.isEmpty()) return;

        for (ActiveBuff buff : buffs) {
            applyBuffToMob(player, mob, buff);
        }
    }

    private static void applyBuffToMob(ServerPlayer player, Mob mob, ActiveBuff buff) {
        switch (buff.buffId()) {
            case "glass_cannon" -> {
                var dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(dmg.getBaseValue() * 3.0);
                var hp = mob.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) {
                    hp.setBaseValue(hp.getBaseValue() * 0.5);
                    mob.setHealth(mob.getMaxHealth());
                }
            }
            case "double_health" -> {
                var hp = mob.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) {
                    hp.setBaseValue(hp.getBaseValue() * 2.0);
                    mob.setHealth(mob.getMaxHealth());
                }
            }
            case "fire_mobs" -> {
                int fireTicks = buff.durationSeconds() * 20;
                mob.setRemainingFireTicks(fireTicks);
            }
            case "invisible_mobs" -> {
                mob.setInvisible(true);
            }
            case "explosive_spawns" -> {
                scheduleExplosion(player, mob);
            }
            case "frostbite" -> {
                tagFrostbite(mob);
            }
            default -> LOGGER.warn("Unknown buff: {}", buff.buffId());
        }
    }

    private static void scheduleExplosion(ServerPlayer player, Mob mob) {
        ServerLevel level = (ServerLevel) player.level();
        int delayTicks = 30;
        level.getServer().executeIfPossible(new TickTask(level.getServer().getTickCount() + delayTicks, () -> {
            if (!mob.isAlive()) return;
            PrimedTnt tnt = new PrimedTnt(level, mob.getX(), mob.getY(), mob.getZ(), player);
            tnt.setFuse(0);
            level.addFreshEntity(tnt);
            level.playSound(null, mob.blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
        }));
    }

    private static void tagFrostbite(Mob mob) {
        mob.addTag(FROSTBITE_TAG);
    }

    public static boolean isFrostbiteMob(Entity entity) {
        return entity != null && entity.entityTags().contains(FROSTBITE_TAG);
    }

    public static String getDisplayName(String buffId) {
        return switch (buffId) {
            case "glass_cannon" -> "Glass Cannon";
            case "frostbite" -> "Frostbite";
            case "double_health" -> "Double Health";
            case "explosive_spawns" -> "Explosive Spawns";
            case "fire_mobs" -> "Fire Mobs";
            case "invisible_mobs" -> "Invisible Mobs";
            default -> buffId;
        };
    }

    public static int getPendingBuffsSize() {
        return pendingBuffs.size();
    }

    public static List<PendingBuff> getPendingBuffsSnapshot() {
        return List.copyOf(pendingBuffs);
    }

    public static void clearPendingBuffs() {
        pendingBuffs.clear();
    }

    public static void resetState() {
        pendingBuffs.clear();
        activeBuffs.clear();
    }
}
