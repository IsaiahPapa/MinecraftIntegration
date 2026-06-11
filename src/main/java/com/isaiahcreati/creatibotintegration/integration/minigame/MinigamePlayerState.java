package com.isaiahcreati.creatibotintegration.integration.minigame;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public class MinigamePlayerState {
    private final double originalX;
    private final double originalY;
    private final double originalZ;
    private final float originalYRot;
    private final float originalXRot;
    private final ResourceKey<Level> originalDimension;
    private final GameType originalGameMode;
    private final long startTick;

    public MinigamePlayerState(ServerPlayer player, long startTick) {
        this.originalX = player.getX();
        this.originalY = player.getY();
        this.originalZ = player.getZ();
        this.originalYRot = player.getYRot();
        this.originalXRot = player.getXRot();
        this.originalDimension = player.level().dimension();
        this.originalGameMode = player.gameMode.getGameModeForPlayer();
        this.startTick = startTick;
    }

    public double getOriginalX() { return originalX; }
    public double getOriginalY() { return originalY; }
    public double getOriginalZ() { return originalZ; }
    public float getOriginalYRot() { return originalYRot; }
    public float getOriginalXRot() { return originalXRot; }
    public ResourceKey<Level> getOriginalDimension() { return originalDimension; }
    public GameType getOriginalGameMode() { return originalGameMode; }
    public long getStartTick() { return startTick; }
}