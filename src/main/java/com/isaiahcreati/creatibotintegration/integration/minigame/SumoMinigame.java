package com.isaiahcreati.creatibotintegration.integration.minigame;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.helpers.Chat;
import com.isaiahcreati.creatibotintegration.integration.SumoArena;
import com.isaiahcreati.creatibotintegration.integration.Taunts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SumoMinigame extends Minigame {

    private final SumoArena arena = new SumoArena();
    private final List<Mob> arenaMobs = new ArrayList<>();
    private static final int GRACE_SECONDS = 5;

    @Override
    public String getId() { return "sumo"; }

    @Override
    public Component getTitle() {
        return Component.literal("Arena").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#55FFFF").getOrThrow()).withBold(true));
    }

    @Override
    public Component getSubtitle() {
        return Component.literal("Knock them off!").setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF").getOrThrow()));
    }

    @Override
    public boolean isEnabled() { return Config.SUMO_ENABLED.get(); }

    @Override
    public boolean hasTimer() { return false; }

    @Override
    public int getDurationSeconds() { return 0; }

    @Override
    public boolean isTimerSurvival() { return false; }

    @Override
    public int getGracePeriodSeconds() { return GRACE_SECONDS; }

    @Override
    public BlockPos getStartPos() { return arena.getStartPosition(); }

    @Override
    public float getFailDamage() { return Config.SUMO_FAIL_DAMAGE.get().floatValue(); }

    @Override
    public void buildArena(ServerLevel level) {
        CreatiIntegration.LOGGER.info("Building Arena course...");
        clearArenaMobs();
        arena.clearMobs(level);
        arena.buildArena(level);
    }

    @Override
    public boolean checkWin(ServerPlayer player) {
        pruneMobs();
        return arenaMobs.isEmpty();
    }

    @Override
    public boolean checkLose(ServerPlayer player) {
        // Player loses if they fall into the water below the platform.
        return player.getY() < arena.getWaterY() + 1;
    }

    @Override
    public void onTick(ServerPlayer player, long currentTick, long elapsedTicks) {
        pruneMobs();

        // Re-aggro mobs onto the player after the grace period ends.
        for (Mob mob : arenaMobs) {
            if (mob.getTarget() == null && mob instanceof net.minecraft.world.entity.monster.Monster monster) {
                monster.setTarget(player);
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
        if (success) {
            Chat.SendAlert(player, "&aYou cleared the Arena!");
        } else {
            Chat.SendAlert(player, "&7You were knocked off the Arena!");
        }
    }

    @Override
    protected void onSessionEnd(ServerPlayer player) {
        clearArenaMobs();
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

        // Spawn the mob ring on the platform edge. Mobs stay frozen until the
        // countdown finishes (see onGameStart).
        BlockPos start = getStartPos();
        int min = Math.max(1, Math.min(3, Config.SUMO_MOB_MIN_COUNT.get()));
        int max = Math.max(min, Math.min(3, Config.SUMO_MOB_MAX_COUNT.get()));
        double ringRadius = Math.max(2.0, arena.getRadius() - 1);
        List<Mob> spawned = Taunts.spawnSumoHostileRing(
                minigameLevel, player,
                start.getX() + 0.5, start.getY(), start.getZ() + 0.5,
                min, max, ringRadius, 0.5);
        arenaMobs.addAll(spawned);

        // Strip knockback resistance from spawned mobs so they can be knocked
        // off the platform easily.
        for (Mob mob : spawned) {
            var kr = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kr != null) kr.setBaseValue(0.0);
            mob.setNoAi(true);
        }
    }

    private void pruneMobs() {
        Iterator<Mob> it = arenaMobs.iterator();
        while (it.hasNext()) {
            Mob mob = it.next();
            if (!mob.isAlive()) {
                it.remove();
                continue;
            }
            // Mobs that fall into the water or below the platform are removed.
            if (mob.getY() < arena.getWaterY() + 1) {
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
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("Arena Sword").withStyle(net.minecraft.ChatFormatting.AQUA));
        ItemStack stick = new ItemStack(Items.STICK);
        stick.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("Knockback Stick").withStyle(net.minecraft.ChatFormatting.YELLOW));
        stick.enchant(player.level().registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK), 2);

        player.getInventory().setItem(0, sword);
        player.getInventory().setItem(1, stick);
        player.getInventory().setSelectedSlot(0);

        // Equip iron armor via equipment slots.
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, namedArmor(Items.IRON_HELMET));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, namedArmor(Items.IRON_CHESTPLATE));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, namedArmor(Items.IRON_LEGGINGS));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, namedArmor(Items.IRON_BOOTS));

        player.inventoryMenu.broadcastChanges();
    }

    private ItemStack namedArmor(Item item) {
        ItemStack piece = new ItemStack(item);
        piece.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("Arena Armor").withStyle(net.minecraft.ChatFormatting.AQUA));
        return piece;
    }
}
