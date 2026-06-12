package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ActivityFeedRenderer {

    public static final Identifier ACTIVITY_FEED_ID =
            Identifier.fromNamespaceAndPath("creatibotintegration", "activity_feed");

    private static final int PADDING = 3;
    private static final int LINE_HEIGHT = 9;
    private static final int TOAST_WIDTH = 140;
    private static final int MAX_VISIBLE = 5;
    private static final int MARGIN_LEFT = 5;
    private static final int MARGIN_TOP = 5;
    private static final int SPACING = 1;

    private static int getEventColor(String eventType) {
        return switch (eventType) {
            case "GIVE" -> 0x55AAFF;
            case "SPAWN" -> 0x55FF55;
            case "EFFECT" -> 0xAA55FF;
            case "TAUNT", "TAUNT_INSTANT" -> 0xFF5555;
            case "TAUNT_QUEUED", "MINIGAME_QUEUED" -> 0xFFAA00;
            case "TAUNT_ACTIVATED", "MINIGAME_ACTIVATED" -> 0x55FF55;
            case "TAUNT_EXTENDED" -> 0xFFFF55;
            case "VISUAL_EFFECT_QUEUED" -> 0xFFAA00;
            case "VISUAL_EFFECT_ACTIVATED" -> 0x55FFFF;
            default -> 0xFFFFFF;
        };
    }

    private static String getStatusLabel(String eventType) {
        return switch (eventType) {
            case "GIVE" -> "\u00a79Give";
            case "SPAWN" -> "\u00a7aSpawn";
            case "EFFECT" -> "\u00a75Effect";
            case "TAUNT", "TAUNT_INSTANT" -> "";
            case "TAUNT_QUEUED" -> "\u00a76Queued";
            case "TAUNT_ACTIVATED" -> "\u00a7aActive";
            case "TAUNT_EXTENDED" -> "\u00a7eExtended";
            case "MINIGAME_QUEUED" -> "\u00a76Queued";
            case "MINIGAME_ACTIVATED" -> "\u00a7aActive";
            case "VISUAL_EFFECT_QUEUED" -> "\u00a76Queued";
            case "VISUAL_EFFECT_ACTIVATED" -> "\u00a7bActive";
            default -> "";
        };
    }

    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!Config.ACTIVITY_FEED_VISIBLE.get()) return;

        List<ActivityFeedState.ActivityToast> toasts = ActivityFeedState.getToasts();
        if (toasts.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Font font = mc.font;
        int y = MARGIN_TOP;
        int count = 0;

        for (ActivityFeedState.ActivityToast toast : toasts) {
            if (count >= MAX_VISIBLE) break;

            float opacity = toast.getOpacity();
            if (opacity <= 0f) continue;

            int alpha = (int)(opacity * 255);
            int bgColor = (alpha << 24);
            int borderColor = (alpha << 24) | (getEventColor(toast.eventType) & 0xFFFFFF);
            int eventColor = getEventColor(toast.eventType);

            String statusLabel = getStatusLabel(toast.eventType);
            String displayName = toast.getDisplayName();

            int lines = 1;
            if (!statusLabel.isEmpty()) lines++;
            if (!toast.redeemerName.isEmpty()) lines++;
            if (toast.queuePosition > 0) lines++;

            int boxHeight = lines * LINE_HEIGHT + PADDING * 2;

            int boxX = MARGIN_LEFT;

            guiGraphics.fill(boxX, y, boxX + TOAST_WIDTH, y + boxHeight, bgColor);
            guiGraphics.fill(boxX, y, boxX + 2, y + boxHeight, borderColor);

            int textX = boxX + PADDING + 2;
            int textY = y + PADDING;

            if (!statusLabel.isEmpty()) {
                guiGraphics.text(font, statusLabel, textX, textY, applyAlpha(0xAAAAAA, alpha), false);
                textY += LINE_HEIGHT;
            }

            guiGraphics.text(font, "\u00a7f" + displayName, textX, textY, applyAlpha(eventColor, alpha), false);
            textY += LINE_HEIGHT;

            if (!toast.redeemerName.isEmpty()) {
                guiGraphics.text(font, "\u00a78by " + toast.redeemerName, textX, textY, applyAlpha(0x888888, alpha), false);
                textY += LINE_HEIGHT;
            }

            if (toast.queuePosition > 0) {
                guiGraphics.text(font, "\u00a78#" + toast.queuePosition, textX, textY, applyAlpha(0x666666, alpha), false);
            }

            y += boxHeight + SPACING;
            count++;
        }
    }

    private static int applyAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}