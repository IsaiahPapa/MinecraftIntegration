package com.isaiahcreati.creatibotintegration.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SafeModeHud {

    public static void render(GuiGraphicsExtractor guiGraphics, Font font, int screenWidth, int screenHeight) {
        if (ClientQueueState.safeModeRemainingSeconds <= 0) return;

        int boxWidth = 110;
        int boxX = screenWidth - boxWidth - 4;
        int y = 4;
        int boxHeight = 16;

        guiGraphics.fill(boxX, y, boxX + boxWidth, y + boxHeight, 0x80FF00AA);
        guiGraphics.fill(boxX, y, boxX + boxWidth, y + 1, 0xFFFF55FF);
        guiGraphics.fill(boxX, y + boxHeight - 1, boxX + boxWidth, y + boxHeight, 0xFFFF55FF);

        String text = "\u00a7f\u00a7l\u2696 Safe Mode \u00a7r\u00a7f" + ClientQueueState.safeModeRemainingSeconds + "s";
        int textX = boxX + (boxWidth - font.width(text)) / 2;
        guiGraphics.text(font, text, textX, y + 4, 0xFFFFFFFF, false);
    }
}