package com.isaiahcreati.creatiintegration.handlers;

import com.isaiahcreati.creatiintegration.helpers.Chat;
import com.isaiahcreati.creatiintegration.screens.ConfigScreen;
import com.isaiahcreati.creatiintegration.toasts.ConfigReminderToast;
import com.mojang.logging.LogUtils;
import com.mojang.text2speech.Narrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.telemetry.events.WorldLoadEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import net.minecraftforge.event.level.LevelEvent;


import java.util.Optional;


@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class EventHandler {
    public static final Logger LOGGER = LogUtils.getLogger();
    private boolean shown = false;


    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {

        Screen screen = event.getScreen();

        if (screen instanceof OptionsScreen) {
            Optional<AbstractWidget> creditsButtonOptional = screen.children().stream()
                    .filter(widget -> widget instanceof Button && ((Button) widget).getMessage().getString().contains("Credits & Attribution")).map(widget -> (AbstractWidget) widget).findAny();

            if (!creditsButtonOptional.isPresent()) return;

            AbstractWidget creditsButton = creditsButtonOptional.get();

            int x = creditsButton.getX() + creditsButton.getWidth() + 5;
            int y = creditsButton.getY();

            Button myButton = Button.builder(Component.literal("Creati's Integration"), b -> {
                Minecraft.getInstance().setScreen(new ConfigScreen(screen));
            }).bounds(x, y, Button.DEFAULT_WIDTH, 20).build();
            screen.renderables.add(myButton);
            event.addListener(myButton);

        }

        if (screen instanceof TitleScreen) {
            Font font = screen.getMinecraft().font;
            Component AlertMessage = Component.literal("Hey! You need to enter in your "); // The button text
            int PADDING_X = 16;
            int PADDING_Y = 64;
            int i = font.width(AlertMessage);
            int j = screen.width - i - 2;


            PlainTextButton PTB = new PlainTextButton(j, screen.height - 30, i, 10, AlertMessage, b -> {
                Minecraft.getInstance().setScreen(new ConfigScreen(screen));
            }, font);

            Button Alert = Button.builder(AlertMessage, b -> {
                Minecraft.getInstance().setScreen(new ConfigScreen(screen));
            }).bounds(screen.width - Button.DEFAULT_WIDTH - PADDING_X, screen.height - Button.DEFAULT_HEIGHT - PADDING_Y, Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT).build();

            screen.renderables.add(PTB);
            event.addListener(PTB);

            screen.renderables.add(Alert);
            event.addListener(Alert);

        }
    }
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event)
    {
        if(shown) return;
        if (!(event.getEntity() instanceof Player player)) return;
        LOGGER.info("Player loaded in! " + player.getName());
        if (!event.getLevel().isClientSide() || this.isConfigSetup()) return;

        Minecraft.getInstance().getToasts().addToast(new ConfigReminderToast(
                Component.literal("Creati's Integration not setup!"),
                Component.literal("Please setup before using.")
        ));

        Component message = Component.literal("[Warning] Creati's Integration not configured!")
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55")));
        player.sendSystemMessage(message);

        shown = true;


    }

    private boolean isConfigSetup() {
        return false;
    }

}
