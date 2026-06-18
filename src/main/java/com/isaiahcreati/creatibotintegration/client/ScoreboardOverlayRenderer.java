package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.network.ClientboundQueueUpdatePacket;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ScoreboardOverlayRenderer {

    public static final Identifier QUEUE_OVERLAY_ID =
            Identifier.fromNamespaceAndPath("creatibotintegration", "queue_overlay");

    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        SafeModeHud.render(guiGraphics, font, screenWidth, screenHeight);

        if (!Config.SIDEBAR_VISIBLE.get()) return;
        if (!ClientQueueState.hasAnythingQueued()) return;

        int y = 0;
        int boxWidth = 120;
        int boxX = screenWidth - boxWidth - 4;

        int totalQueueSize = ClientQueueState.minigameQueue.size()
                + ClientQueueState.visualEffectQueue.size()
                + ClientQueueState.pendingTauntsCount;

        int lines = 1;
        if (!ClientQueueState.activeMinigameId.isEmpty()) lines++;
        if (!ClientQueueState.activeVisualEffectId.isEmpty()) lines++;
        if (ClientQueueState.pausedEffectRemainingSeconds > 0) lines++;
        if (totalQueueSize > 0) lines++;

        int lineHeight = 11;
        int boxHeight = lines * lineHeight + 8;

        y = (screenHeight - boxHeight) / 2;

        guiGraphics.fill(boxX, y, boxX + boxWidth, y + boxHeight, 0x70000000);

        int textX = boxX + boxWidth / 2;
        y += 4;

        String title = "\u00a7b\u00a7lCreati's Queue";
        guiGraphics.text(font, title, textX - font.width(title) / 2, y, 0xFFFFFFFF, false);
        y += lineHeight;

        if (!ClientQueueState.activeMinigameId.isEmpty()) {
            String name = ClientQueueState.getDisplayName(ClientQueueState.activeMinigameId);
            String line = "\u00a7a" + name;
            guiGraphics.text(font, line, textX - font.width(line) / 2, y, 0xFFFFFFFF, false);
            y += lineHeight;
        }

        if (!ClientQueueState.activeVisualEffectId.isEmpty()) {
            String name = ClientQueueState.getDisplayName(ClientQueueState.activeVisualEffectId);
            String timeStr = ClientQueueState.activeVisualEffectRemainingSeconds > 0
                    ? " " + ClientQueueState.activeVisualEffectRemainingSeconds + "s" : "";
            String line = "\u00a7e" + name + timeStr;
            guiGraphics.text(font, line, textX - font.width(line) / 2, y, 0xFFFFFFFF, false);

            if (ClientQueueState.activeVisualEffectRemainingSeconds > 0 && ClientQueueState.activeVisualEffectDurationSeconds > 0) {
                y += lineHeight;
                int barX = boxX + 6;
                int barWidth = boxWidth - 12;
                int barY = y;
                int barHeight = 3;
                float progress = (float) ClientQueueState.activeVisualEffectRemainingSeconds
                        / ClientQueueState.activeVisualEffectDurationSeconds;
                progress = Math.max(0f, Math.min(1f, progress));
                guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x50000000);
                int filledWidth = (int)(barWidth * progress);
                if (filledWidth > 0) {
                    int barColor = progress > 0.25f ? 0xFF55FF55 : 0xFFFF5555;
                    guiGraphics.fill(barX, barY, barX + filledWidth, barY + barHeight, barColor);
                }
            }
            y += lineHeight;
        }

        if (ClientQueueState.pausedEffectRemainingSeconds > 0) {
            String line = "\u00a76\u23f8 Paused (" + ClientQueueState.pausedEffectRemainingSeconds + "s)";
            guiGraphics.text(font, line, textX - font.width(line) / 2, y, 0xFFFFFFFF, false);
            y += lineHeight;
        }

        if (totalQueueSize > 0) {
            String line = "\u00a77" + totalQueueSize + " queued";
            guiGraphics.text(font, line, textX - font.width(line) / 2, y, 0xFFFFFFFF, false);
        }
    }
}