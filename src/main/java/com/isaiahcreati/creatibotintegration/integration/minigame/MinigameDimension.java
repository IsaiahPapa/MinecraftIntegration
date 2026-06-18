package com.isaiahcreati.creatibotintegration.integration.minigame;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class MinigameDimension {

    public static final ResourceKey<Level> MINIGAME_LEVEL_KEY = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("creatibotintegration", "minigame_arena")
    );

    public static ServerLevel getMinigameLevel(ServerPlayer player) {
        return player.level().getServer().getLevel(MINIGAME_LEVEL_KEY);
    }

    public static boolean isMinigameDimension(Level level) {
        return level.dimension().equals(MINIGAME_LEVEL_KEY);
    }
}