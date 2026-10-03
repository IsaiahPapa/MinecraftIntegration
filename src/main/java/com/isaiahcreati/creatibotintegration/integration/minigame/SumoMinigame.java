package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.integration.SumoArena;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SumoMinigame extends Minigame {

    private static final int COUNTDOWN_SECONDS = 3;

    /** Marks mobs spawned by this game so their loot and XP can be suppressed. */
    public static final String ARENA_MOB_TAG = "creatibotintegration_arena_mob";

    // Mobs that read clearly at close range and take knockback well. Spiders
    // are deliberately excluded: one knocked into the gap would climb the wall
    // and could never be reached, so the round could never be won.
    private static final List<EntityType<? extends Monster>> MOB_POOL = List.of(
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON);

    private final SumoArena arena = new SumoArena();
    private final List<Mob> arenaMobs = new ArrayList<>();
    private final Random random = new Random();

    @Override
    public String getId() { return "sumo"; }

    @Override
    public Component getTitle() {
        return Component.literal("Arena").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#55FFFF").getOrThrow()).withBold(true));
    }

    @Override
    public Component getSubtitle() {
        return Component.literal("Knock them off, or survive " + getDurationSeconds() + " seconds!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF").getOrThrow()));
    }

    @Override
    public boolean isEnabled() { return Config.SUMO_ENABLED.get(); }

    // A time limit guarantees the round ends even if a mob gets stuck
    // somewhere the player can't reach. Outlasting it counts as a win.
    @Override
    public boolean hasTimer() { return true; }

    @Override
    public int getDurationSeconds() { return Config.SUMO_DURATION_SECONDS.get(); }

    @Override
    public boolean isTimerSurvival() { return true; }

    @Override
    public int getGracePeriodSeconds() { return COUNTDOWN_SECONDS; }

    @Override
    protected double getVoidY() { return arena.getKnockoutY(); }

    @Override
    public BlockPos getStartPos() { return arena.getStartPosition(); }

    @Override
    public float getFailDamage() { return Config.SUMO_FAIL_DAMAGE.get().floatValue(); }

    @Override
    protected boolean arenaNeedsRebuild() {
        return super.arenaNeedsRebuild() || !arena.isBuiltForCurrentConfig();
    }

    @Override
    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building Arena course...");
        clearArenaMobs();
        arena.clearMobs(level);
        arena.buildArena(level);
    }

    @Override
    protected boolean canStart(ServerPlayer player) {
        // Peaceful despawns hostile mobs on their first tick, which would hand
        // out an instant win. Skip the game so the queue moves on.
        if (player.level().getDifficulty() == Difficulty.PEACEFUL) {
            Chat.SendAlert(player, "&7The Arena needs a difficulty above Peaceful, so it was skipped.");
            return false;
        }
        return true;
    }

    @Override
    protected String getStatusText(ServerPlayer player, int remainingSeconds) {
        return "Mobs left: " + arenaMobs.size() + "  |  " + super.getStatusText(player, remainingSeconds);
    }

    @Override
    public boolean checkWin(ServerPlayer player) {
        pruneMobs();
        return arenaMobs.isEmpty();
    }

    @Override
    public boolean checkLose(ServerPlayer player) { return false; }

    @Override
    public void onTick(ServerPlayer player, long currentTick, long elapsedTicks) {
        for (Mob mob : arenaMobs) {
            if (mob.getTarget() == null) {
                mob.setTarget(player);
            }
        }
    }

    @Override
    public void onPlayerFall(ServerPlayer player) {
        exitPlayer(player, false);
    }

    @Override
    protected AABB getArenaBounds() {
        return arena.getBounds();
    }

    @Override
    protected void onExit(ServerPlayer player, boolean success) {
        if (!success) {
            Chat.SendAlert(player, "&7You were knocked off the Arena!");
        } else if (arenaMobs.isEmpty()) {
            Chat.SendAlert(player, "&aYou cleared the Arena!");
        } else {
            Chat.SendAlert(player, "&aYou held your ground in the Arena!");
        }
    }

    @Override
    protected void onSessionEnd(ServerPlayer player) {
        clearArenaMobs();
        ServerLevel level = MinigameDimension.getMinigameLevel(player);
        if (level != null) {
            // Also sweeps anything untracked, e.g. a zombie reinforcement.
            arena.clearMobs(level);
        }
    }

    @Override
    protected void onGameStart(ServerPlayer player) {
        for (Mob mob : arenaMobs) {
            mob.setNoAi(false);
            mob.setTarget(player);
        }
    }

    @Override
    protected void onSessionStart(ServerPlayer player, ServerLevel minigameLevel, MinigamePlayerState state) {
        // Purge untracked mobs left by a disconnect, restart, or interrupted
        // session before creating exactly one new wave.
        clearArenaMobs();
        arena.clearMobs(minigameLevel);

        // Snapshot the player's inventory, then clear it and give a kit. The
        // base class restores the snapshot however the session ends.
        state.setSavedInventory(snapshotInventory(player));
        clearInventory(player);
        giveKit(player);

        // Spawn a ring of mobs around the player, close enough to engage
        // straight away rather than on the very edge where they would wander
        // off on their own. They stay frozen until the countdown finishes.
        BlockPos start = getStartPos();
        int min = Math.max(1, Math.min(3, Config.SUMO_MOB_MIN_COUNT.get()));
        int max = Math.max(min, Math.min(3, Config.SUMO_MOB_MAX_COUNT.get()));
        int count = min + random.nextInt(max - min + 1);
        double ringRadius = Math.max(2.5, arena.getRadius() * 0.6);
        double angleOffset = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < count; i++) {
            double angle = angleOffset + (Math.PI * 2 * i) / count;
            Mob mob = spawnArenaMob(minigameLevel,
                    start.getX() + 0.5 + Math.cos(angle) * ringRadius,
                    start.getY(),
                    start.getZ() + 0.5 + Math.sin(angle) * ringRadius);
            if (mob != null) {
                arenaMobs.add(mob);
            }
        }
        minigameLevel.playSound(null, start, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.6F, 0.8F);
    }

    private Mob spawnArenaMob(ServerLevel level, double x, double y, double z) {
        EntityType<? extends Monster> type = MOB_POOL.get(random.nextInt(MOB_POOL.size()));
        Monster mob = type.create(level, EntitySpawnReason.EVENT);
        if (mob == null) return null;

        BlockPos center = getStartPos();
        float yaw = (float) (Mth.atan2(center.getZ() + 0.5 - z, center.getX() + 0.5 - x) * Mth.RAD_TO_DEG) - 90.0F;
        mob.snapTo(x, y, z, yaw, 0.0F);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);

        // Adult zombies only: babies are hard to hit and can spawn as chicken
        // jockeys, which would add an untracked mount to the arena.
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.EVENT,
                mob instanceof Zombie ? new Zombie.ZombieGroupData(false, false) : null);
        if (mob.isPassenger()) {
            Entity vehicle = mob.getVehicle();
            mob.stopRiding();
            if (vehicle != null) vehicle.discard();
        }

        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
        }
        mob.addTag(ARENA_MOB_TAG);

        // No knockback resistance, so every mob can be pushed off the edge.
        AttributeInstance knockbackResistance = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) knockbackResistance.setBaseValue(0.0);
        // Zombies can call reinforcements when hit; those would be untracked.
        AttributeInstance reinforcements = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
        if (reinforcements != null) reinforcements.setBaseValue(0.0);

        mob.setNoAi(true);
        level.addFreshEntity(mob);
        return mob;
    }

    private void pruneMobs() {
        Iterator<Mob> it = arenaMobs.iterator();
        while (it.hasNext()) {
            Mob mob = it.next();
            if (!mob.isAlive()) {
                it.remove();
                continue;
            }
            // Count a mob as knocked off as soon as it drops below the platform.
            if (mob.getY() < arena.getKnockoutY()) {
                mob.level().playSound(null, mob.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 1.0F, 1.0F);
                mob.discard();
                it.remove();
            }
        }
    }

    private void clearArenaMobs() {
        for (Mob mob : arenaMobs) {
            if (mob.isAlive()) mob.discard();
        }
        arenaMobs.clear();
    }

    private List<ItemStack> snapshotInventory(ServerPlayer player) {
        List<ItemStack> snapshot = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            snapshot.add(player.getInventory().getItem(i).copy());
        }
        return snapshot;
    }

    private void clearInventory(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            player.getInventory().setItem(i, ItemStack.EMPTY);
        }
        player.inventoryMenu.broadcastChanges();
    }

    private void giveKit(ServerPlayer player) {
        ItemStack stick = new ItemStack(Items.STICK);
        stick.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("Knockback Stick").withStyle(net.minecraft.ChatFormatting.YELLOW));
        stick.enchant(player.level().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK), 2);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("Arena Sword").withStyle(net.minecraft.ChatFormatting.AQUA));

        // The stick is the main tool in a knock-off game, so start holding it.
        player.getInventory().setItem(0, stick);
        player.getInventory().setItem(1, sword);
        player.getInventory().setSelectedSlot(0);

        player.setItemSlot(EquipmentSlot.HEAD, namedArmor(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, namedArmor(Items.IRON_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, namedArmor(Items.IRON_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, namedArmor(Items.IRON_BOOTS));

        player.inventoryMenu.broadcastChanges();
    }

    private ItemStack namedArmor(Item item) {
        ItemStack piece = new ItemStack(item);
        piece.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("Arena Armor").withStyle(net.minecraft.ChatFormatting.AQUA));
        return piece;
    }
}
