package com.isaiahcreati.creatibotintegration.integration.minigame;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Everything needed to return a player to exactly where they were before a
 * minigame. The state is also written to the player's persistent data so a
 * crash or save-and-quit mid-game can still be undone on the next login.
 */
public class MinigamePlayerState {
    private final double originalX;
    private final double originalY;
    private final double originalZ;
    private final float originalYRot;
    private final float originalXRot;
    private final ResourceKey<Level> originalDimension;
    private final GameType originalGameMode;
    private final float originalHealth;
    private final float originalAbsorption;
    private final int originalFoodLevel;
    private final float originalSaturation;
    private final List<MobEffectInstance> originalEffects;
    private final long startTick;
    private List<ItemStack> savedInventory;
    private boolean gameStarted;

    public MinigamePlayerState(ServerPlayer player, long startTick) {
        this.originalX = player.getX();
        this.originalY = player.getY();
        this.originalZ = player.getZ();
        this.originalYRot = player.getYRot();
        this.originalXRot = player.getXRot();
        this.originalDimension = player.level().dimension();
        this.originalGameMode = player.gameMode.getGameModeForPlayer();
        this.originalHealth = player.getHealth();
        this.originalAbsorption = player.getAbsorptionAmount();
        this.originalFoodLevel = player.getFoodData().getFoodLevel();
        this.originalSaturation = player.getFoodData().getSaturationLevel();
        this.originalEffects = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            this.originalEffects.add(new MobEffectInstance(effect));
        }
        this.startTick = startTick;
    }

    private MinigamePlayerState(CompoundTag tag, RegistryOps<Tag> ops) {
        this.originalX = tag.getDoubleOr("x", 0);
        this.originalY = tag.getDoubleOr("y", 0);
        this.originalZ = tag.getDoubleOr("z", 0);
        this.originalYRot = tag.getFloatOr("yRot", 0);
        this.originalXRot = tag.getFloatOr("xRot", 0);
        this.originalDimension = ResourceKey.create(Registries.DIMENSION,
                Identifier.parse(tag.getStringOr("dimension", Level.OVERWORLD.identifier().toString())));
        this.originalGameMode = GameType.byId(tag.getIntOr("gameMode", GameType.SURVIVAL.getId()));
        this.originalHealth = tag.getFloatOr("health", 20);
        this.originalAbsorption = tag.getFloatOr("absorption", 0);
        this.originalFoodLevel = tag.getIntOr("food", 20);
        this.originalSaturation = tag.getFloatOr("saturation", 5);
        this.originalEffects = new ArrayList<>(
                tag.read("effects", MobEffectInstance.CODEC.listOf(), ops).orElse(List.of()));
        this.savedInventory = tag.read("inventory", ItemStack.OPTIONAL_CODEC.listOf(), ops)
                .map(ArrayList::new)
                .orElse(null);
        this.startTick = 0;
    }

    public CompoundTag toTag(HolderLookup.Provider registries) {
        RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        CompoundTag tag = new CompoundTag();
        tag.putDouble("x", originalX);
        tag.putDouble("y", originalY);
        tag.putDouble("z", originalZ);
        tag.putFloat("yRot", originalYRot);
        tag.putFloat("xRot", originalXRot);
        tag.putString("dimension", originalDimension.identifier().toString());
        tag.putInt("gameMode", originalGameMode.getId());
        tag.putFloat("health", originalHealth);
        tag.putFloat("absorption", originalAbsorption);
        tag.putInt("food", originalFoodLevel);
        tag.putFloat("saturation", originalSaturation);
        tag.store("effects", MobEffectInstance.CODEC.listOf(), ops, originalEffects);
        if (savedInventory != null) {
            tag.store("inventory", ItemStack.OPTIONAL_CODEC.listOf(), ops, savedInventory);
        }
        return tag;
    }

    public static Optional<MinigamePlayerState> fromTag(CompoundTag tag, HolderLookup.Provider registries) {
        try {
            return Optional.of(new MinigamePlayerState(tag, registries.createSerializationContext(NbtOps.INSTANCE)));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public double getOriginalX() { return originalX; }
    public double getOriginalY() { return originalY; }
    public double getOriginalZ() { return originalZ; }
    public float getOriginalYRot() { return originalYRot; }
    public float getOriginalXRot() { return originalXRot; }
    public ResourceKey<Level> getOriginalDimension() { return originalDimension; }
    public GameType getOriginalGameMode() { return originalGameMode; }
    public float getOriginalHealth() { return originalHealth; }
    public float getOriginalAbsorption() { return originalAbsorption; }
    public int getOriginalFoodLevel() { return originalFoodLevel; }
    public float getOriginalSaturation() { return originalSaturation; }
    public List<MobEffectInstance> getOriginalEffects() { return originalEffects; }
    public long getStartTick() { return startTick; }

    /** Inventory to put back when the session ends, or null if it was never swapped out. */
    public List<ItemStack> getSavedInventory() { return savedInventory; }
    public void setSavedInventory(List<ItemStack> savedInventory) { this.savedInventory = savedInventory; }

    public boolean isGameStarted() { return gameStarted; }
    public void setGameStarted(boolean gameStarted) { this.gameStarted = gameStarted; }
}
