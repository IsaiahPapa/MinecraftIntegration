package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ScoreboardOverlayRenderer {

    public static final Identifier QUEUE_OVERLAY_ID =
            Identifier.fromNamespaceAndPath("creatibotintegration", "queue_overlay");

    private static final int MAX_VISIBLE_QUEUE_ENTRIES = 4;
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 11;
    private static final int SECTION_HEIGHT = 9;
    private static final int EFFECT_BAR_HEIGHT = 5;
    private static final int PREFERRED_BOX_WIDTH = 166;

    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        SafeModeHud.render(guiGraphics, font, screenWidth, screenHeight);

        if (!Config.QUEUE_ENABLED.get() || !Config.SIDEBAR_VISIBLE.get()) return;
        if (!ClientQueueState.hasAnythingQueued()) return;

        boolean hasActiveMinigame = !ClientQueueState.activeMinigameId.isEmpty();
        boolean hasActiveEffect = !ClientQueueState.activeVisualEffectId.isEmpty();
        boolean hasPausedEffect = !ClientQueueState.pausedVisualEffectId.isEmpty()
                || ClientQueueState.pausedEffectRemainingSeconds > 0;
        int totalQueueSize = ClientQueueState.getTotalQueueSize();
        List<QueueViewModel.DisplayEntry> visibleEntries =
                ClientQueueState.getVisibleQueueEntries(MAX_VISIBLE_QUEUE_ENTRIES);
        int hiddenEntries = Math.max(0, totalQueueSize - visibleEntries.size());

        int contentHeight = LINE_HEIGHT;
        if (hasActiveMinigame || hasActiveEffect || hasPausedEffect) {
            contentHeight += SECTION_HEIGHT;
            if (hasActiveMinigame) contentHeight += LINE_HEIGHT;
            if (hasActiveEffect) {
                contentHeight += LINE_HEIGHT;
                if (ClientQueueState.activeVisualEffectRemainingSeconds > 0
                        && ClientQueueState.activeVisualEffectDurationSeconds > 0) {
                    contentHeight += EFFECT_BAR_HEIGHT;
                }
            }
            if (hasPausedEffect) contentHeight += LINE_HEIGHT;
        }
        if (totalQueueSize > 0) {
            contentHeight += SECTION_HEIGHT + visibleEntries.size() * LINE_HEIGHT;
            if (hiddenEntries > 0) contentHeight += LINE_HEIGHT;
        }

        int boxWidth = Math.min(PREFERRED_BOX_WIDTH, Math.max(100, screenWidth - 8));
        int boxHeight = contentHeight + PADDING * 2;
        int boxX = screenWidth - boxWidth - 4;
        int y = (screenHeight - boxHeight) / 2;

        guiGraphics.fill(boxX, y, boxX + boxWidth, y + boxHeight, 0x78000000);
        guiGraphics.fill(boxX, y, boxX + 2, y + boxHeight, 0xCC22AADD);

        int textX = boxX + PADDING + 2;
        int maxTextWidth = boxWidth - PADDING * 2 - 4;
        int centerX = boxX + boxWidth / 2;
        y += PADDING;

        String title = "Creati's Queue";
        guiGraphics.text(font, title, centerX - font.width(title) / 2, y, 0xFF55FFFF, false);
        y += LINE_HEIGHT;

        if (hasActiveMinigame || hasActiveEffect || hasPausedEffect) {
            guiGraphics.text(font, "ACTIVE", textX, y, 0xFF888888, false);
            y += SECTION_HEIGHT;

            if (hasActiveMinigame) {
                String line = formatLine("GAME", ClientQueueState.getDisplayName(ClientQueueState.activeMinigameId),
                        ClientQueueState.activeMinigameRedeemer, "");
                guiGraphics.text(font, fitToWidth(font, line, maxTextWidth), textX, y, 0xFF55FF55, false);
                y += LINE_HEIGHT;
            }

            if (hasActiveEffect) {
                String time = ClientQueueState.activeVisualEffectRemainingSeconds > 0
                        ? ClientQueueState.activeVisualEffectRemainingSeconds + "s" : "";
                String line = formatLine("FX", ClientQueueState.getDisplayName(ClientQueueState.activeVisualEffectId),
                        ClientQueueState.activeVisualEffectRedeemer, time);
                guiGraphics.text(font, fitToWidth(font, line, maxTextWidth), textX, y, 0xFFFFFF55, false);
                y += LINE_HEIGHT;

                if (ClientQueueState.activeVisualEffectRemainingSeconds > 0
                        && ClientQueueState.activeVisualEffectDurationSeconds > 0) {
                    int barWidth = maxTextWidth;
                    float progress = (float) ClientQueueState.activeVisualEffectRemainingSeconds
                            / ClientQueueState.activeVisualEffectDurationSeconds;
                    progress = Math.max(0f, Math.min(1f, progress));
                    guiGraphics.fill(textX, y, textX + barWidth, y + 3, 0x60000000);
                    int filledWidth = (int) (barWidth * progress);
                    if (filledWidth > 0) {
                        int barColor = progress > 0.25f ? 0xFF55FF55 : 0xFFFF5555;
                        guiGraphics.fill(textX, y, textX + filledWidth, y + 3, barColor);
                    }
                    y += EFFECT_BAR_HEIGHT;
                }
            }

            if (hasPausedEffect) {
                String pausedName = ClientQueueState.pausedVisualEffectId.isEmpty()
                        ? "Visual Effect"
                        : ClientQueueState.getDisplayName(ClientQueueState.pausedVisualEffectId);
                String time = ClientQueueState.pausedEffectRemainingSeconds > 0
                        ? ClientQueueState.pausedEffectRemainingSeconds + "s" : "";
                String line = formatLine("PAUSED", pausedName,
                        ClientQueueState.pausedVisualEffectRedeemer, time);
                guiGraphics.text(font, fitToWidth(font, line, maxTextWidth), textX, y, 0xFFFFAA00, false);
                y += LINE_HEIGHT;
            }
        }

        if (totalQueueSize > 0) {
            guiGraphics.text(font, "UP NEXT (" + totalQueueSize + ")", textX, y, 0xFF888888, false);
            y += SECTION_HEIGHT;

            for (QueueViewModel.DisplayEntry displayEntry : visibleEntries) {
                var entry = displayEntry.entry();
                String name = entry.displayName().isEmpty()
                        ? ClientQueueState.getDisplayName(entry.id())
                        : entry.displayName();
                String line = formatLine(displayEntry.category().label(), name, entry.redeemerName(), "");
                guiGraphics.text(font, fitToWidth(font, line, maxTextWidth), textX, y,
                        0xFF000000 | displayEntry.category().color(), false);
                y += LINE_HEIGHT;
            }

            if (hiddenEntries > 0) {
                guiGraphics.text(font, "+" + hiddenEntries + " more", textX, y, 0xFF888888, false);
            }
        }
    }

    private static String formatLine(String category, String name, String redeemer, String suffix) {
        StringBuilder line = new StringBuilder(category).append("  ").append(name);
        if (!suffix.isEmpty()) line.append("  ").append(suffix);
        if (redeemer != null && !redeemer.isBlank()) line.append(" · ").append(redeemer);
        return line.toString();
    }

    private static String fitToWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int end = text.length();
        while (end > 0 && font.width(text.substring(0, end) + ellipsis) > maxWidth) {
            end--;
        }
        return text.substring(0, end).stripTrailing() + ellipsis;
    }
}
