package com.isaiahcreati.creatibotintegration.screens;

import com.isaiahcreati.creatibotintegration.network.ServerboundOnboardingPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class OnboardingScreen extends Screen {

    private static final int BG_COLOR = 0xFF1E1E2E;
    private static final int ACCENT_COLOR = 0xFF55AAFF;
    private static final int TEXT_COLOR = 0xFFAAAAAA;
    private static final int HEADER_COLOR = 0xFFFFFFFF;

    private int currentPage = 0;
    private static final int TOTAL_PAGES = 5;

    private EditBox alertKeyField;
    private boolean chatAlerts = true;
    private boolean minigamesEnabled = true;

    private Button nextButton;
    private Button backButton;
    private Button closeButton;
    private Button doneButton;
    private Button toggleChatAlertsButton;
    private Button toggleMinigamesButton;

    public OnboardingScreen() {
        super(Component.literal("CreatiBot Setup"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = Math.min(320, this.width - 40);
        int panelHeight = Math.min(220, this.height - 40);
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;

        int bottomY = panelTop + panelHeight - 30;
        int fieldY = panelTop + 65;

        alertKeyField = new EditBox(this.font, centerX - 100, fieldY, 200, 20, Component.literal("Alert Key"));
        alertKeyField.setMaxLength(256);
        alertKeyField.setHint(Component.literal("Enter your Alert Key...").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#666666").getOrThrow())));
        alertKeyField.setVisible(false);
        this.addRenderableWidget(alertKeyField);

        nextButton = Button.builder(Component.literal("Next"), btn -> {
            if (currentPage < TOTAL_PAGES - 1) {
                currentPage++;
                updateVisibility();
            }
        }).bounds(centerX + 5, bottomY, 80, 20).build();
        this.addRenderableWidget(nextButton);

        backButton = Button.builder(Component.literal("Back"), btn -> {
            if (currentPage > 0) {
                currentPage--;
                updateVisibility();
            }
        }).bounds(centerX - 85, bottomY, 80, 20).build();
        this.addRenderableWidget(backButton);

        closeButton = Button.builder(Component.literal("\u2715"), btn -> {
            sendOnboardingPacket();
            this.onClose();
        }).bounds(panelLeft + panelWidth - 22, panelTop + 4, 18, 18).build();
        this.addRenderableWidget(closeButton);

        doneButton = Button.builder(Component.literal("Done"), btn -> {
            sendOnboardingPacket();
            this.onClose();
        }).bounds(centerX - 40, bottomY, 80, 20).build();
        this.addRenderableWidget(doneButton);

        toggleChatAlertsButton = Button.builder(
                Component.literal(chatAlerts ? "\u2714 Yes" : "\u2718 No"),
                btn -> {
                    chatAlerts = !chatAlerts;
                    btn.setMessage(Component.literal(chatAlerts ? "\u2714 Yes" : "\u2718 No"));
                }
        ).bounds(centerX - 40, fieldY, 80, 20).build();
        this.addRenderableWidget(toggleChatAlertsButton);

        toggleMinigamesButton = Button.builder(
                Component.literal(minigamesEnabled ? "\u2714 Enabled" : "\u2718 Disabled"),
                btn -> {
                    minigamesEnabled = !minigamesEnabled;
                    btn.setMessage(Component.literal(minigamesEnabled ? "\u2714 Enabled" : "\u2718 Disabled"));
                }
        ).bounds(centerX - 50, fieldY + 80, 100, 20).build();
        this.addRenderableWidget(toggleMinigamesButton);

        updateVisibility();
    }

    private void updateVisibility() {
        alertKeyField.setVisible(currentPage == 1);
        if (currentPage == 1) {
            alertKeyField.setFocused(true);
        } else {
            alertKeyField.setFocused(false);
        }

        boolean hasBack = currentPage > 0;
        nextButton.visible = currentPage < TOTAL_PAGES - 1;
        backButton.visible = hasBack;
        doneButton.visible = currentPage == TOTAL_PAGES - 1;
        toggleChatAlertsButton.visible = currentPage == 2;
        toggleMinigamesButton.visible = currentPage == 3;

        int centerX = this.width / 2;
        int panelWidth = Math.min(320, this.width - 40);
        int panelHeight = Math.min(220, this.height - 40);
        int panelTop = this.height / 2 - panelHeight / 2;
        int bottomY = panelTop + panelHeight - 30;

        if (hasBack) {
            nextButton.setX(centerX + 5);
            backButton.setX(centerX - 85);
        } else {
            nextButton.setX(centerX - 40);
        }
        nextButton.setY(bottomY);
        backButton.setY(bottomY);
        doneButton.setY(bottomY);
    }

    private void sendOnboardingPacket() {
        ServerboundOnboardingPacket packet = new ServerboundOnboardingPacket(
                alertKeyField.getValue(),
                chatAlerts,
                minigamesEnabled
        );
        ClientPacketDistributor.sendToServer(packet);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int panelWidth = Math.min(320, this.width - 40);
        int panelHeight = Math.min(220, this.height - 40);
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = centerY - panelHeight / 2;

        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, BG_COLOR);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 3, ACCENT_COLOR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int panelWidth = Math.min(320, this.width - 40);
        int panelHeight = Math.min(220, this.height - 40);
        int panelLeft = centerX - panelWidth / 2;
        int panelTop = this.height / 2 - panelHeight / 2;
        int textY = panelTop + 20;
        int textWidth = panelWidth - 40;

        switch (currentPage) {
            case 0 -> renderPage0(graphics, centerX, textY, textWidth);
            case 1 -> renderPage1(graphics, centerX, textY, textWidth);
            case 2 -> renderPage2(graphics, centerX, textY, textWidth);
            case 3 -> renderPage3(graphics, centerX, textY, textWidth);
            case 4 -> renderPage4(graphics, centerX, textY, textWidth);
        }

        renderPaginationDots(graphics, centerX, panelTop + panelHeight - 50, panelWidth);
    }

    private void renderCenteredText(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color, boolean shadow) {
        int width = this.font.width(text);
        graphics.text(this.font, text, centerX - width / 2, y, color, shadow);
    }

    private void renderCenteredWordWrap(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int maxWidth, int color) {
        int leftX = centerX - maxWidth / 2;
        graphics.textWithWordWrap(this.font, text, leftX, y, maxWidth, color);
    }

    private void renderPage0(GuiGraphicsExtractor graphics, int centerX, int y, int width) {
        renderCenteredText(graphics, "Welcome to CreatiBot Integration!", centerX, y, ACCENT_COLOR, true);
        renderCenteredWordWrap(graphics, Component.literal("This mod connects your Minecraft world to Creati's Bot, enabling interactive stream taunts and minigames for your viewers.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").getOrThrow())), centerX, y + 30, width, TEXT_COLOR);
        renderCenteredWordWrap(graphics, Component.literal("Viewers can trigger fun effects, spawn mobs, and send you into minigames \u2014 all through stream interactions.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").getOrThrow())), centerX, y + 65, width, TEXT_COLOR);
        renderCenteredText(graphics, "Let's get you set up!", centerX, y + 110, HEADER_COLOR, false);
    }

    private void renderPage1(GuiGraphicsExtractor graphics, int centerX, int y, int width) {
        renderCenteredText(graphics, "Alert Key", centerX, y, ACCENT_COLOR, true);
        renderCenteredWordWrap(graphics, Component.literal("Enter your Alert Key from Creati's Bot dashboard. This connects your Minecraft server to the bot so it can receive events.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").getOrThrow())), centerX, y + 16, width, TEXT_COLOR);
        renderCenteredText(graphics, "You can skip this and set it later via /creati setup.", centerX, y + 70, TEXT_COLOR, false);
    }

    private void renderPage2(GuiGraphicsExtractor graphics, int centerX, int y, int width) {
        renderCenteredText(graphics, "Chat Alerts", centerX, y, ACCENT_COLOR, true);
        renderCenteredWordWrap(graphics, Component.literal("Would you like to see chat notifications when events happen? These alert you when viewers trigger taunts or interact with the bot.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").getOrThrow())), centerX, y + 16, width, TEXT_COLOR);
    }

    private void renderPage3(GuiGraphicsExtractor graphics, int centerX, int y, int width) {
        renderCenteredText(graphics, "Minigames", centerX, y, ACCENT_COLOR, true);
        renderCenteredWordWrap(graphics, Component.literal("Minigames are special challenges triggered by stream viewers. Players are teleported to an arena and must complete an objective to escape.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").getOrThrow())), centerX, y + 16, width, TEXT_COLOR);

        renderCenteredText(graphics, "\u00A76Parkour \u00A77\u2014 Jump across platforms to the finish.", centerX, y + 50, TEXT_COLOR, false);
        renderCenteredText(graphics, "\u00A74TNT Run \u00A77\u2014 Survive on crumbling floors.", centerX, y + 65, TEXT_COLOR, false);
        renderCenteredText(graphics, "\u00A79Dropper \u00A77\u2014 Fall through a tube and land in water.", centerX, y + 80, TEXT_COLOR, false);
    }

    private void renderPage4(GuiGraphicsExtractor graphics, int centerX, int y, int width) {
        renderCenteredText(graphics, "All Set!", centerX, y, ACCENT_COLOR, true);
        renderCenteredWordWrap(graphics, Component.literal("You're configured and ready to go! You can always re-run /creati setup to change settings, or use the full config screen for advanced options.").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").getOrThrow())), centerX, y + 16, width, TEXT_COLOR);
        renderCenteredText(graphics, "Have fun streaming!", centerX, y + 70, HEADER_COLOR, false);
    }

    private void renderPaginationDots(GuiGraphicsExtractor graphics, int centerX, int y, int panelWidth) {
        int dotSpacing = 12;
        int totalWidth = (TOTAL_PAGES - 1) * dotSpacing;
        int startX = centerX - totalWidth / 2;

        for (int i = 0; i < TOTAL_PAGES; i++) {
            int color = i == currentPage ? ACCENT_COLOR : 0xFF555555;
            int dotX = startX + i * dotSpacing;
            graphics.fill(dotX - 2, y - 2, dotX + 2, y + 2, color);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}