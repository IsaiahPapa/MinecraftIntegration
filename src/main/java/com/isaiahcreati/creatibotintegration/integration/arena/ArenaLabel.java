package com.isaiahcreati.creatibotintegration.integration.arena;

import net.minecraft.network.chat.Component;

/** A floating line of text placed with an arena. Coordinates are the text's center. */
public record ArenaLabel(double x, double y, double z, Component text, float scale) {

    public static ArenaLabel of(double x, double y, double z, Component text) {
        return new ArenaLabel(x, y, z, text, 1.0F);
    }
}
