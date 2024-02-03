package com.isaiahcreati.creatiintegration.handlers;

import com.isaiahcreati.creatiintegration.screens.ConfigScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Optional;


@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class EventHandler {
    public static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {

        Screen screen = event.getScreen();

        if (screen instanceof OptionsScreen) {
            // Find the credits button by its message
            Optional<AbstractWidget> creditsButtonOptional = screen.children().stream()
                    .filter(widget -> widget instanceof Button && ((Button) widget).getMessage().getString().contains("Credits & Attribution")).map(widget -> (AbstractWidget) widget).findAny();

            if (!creditsButtonOptional.isPresent()) return;

            AbstractWidget creditsButton = creditsButtonOptional.get();

            int x = creditsButton.getX() + creditsButton.getWidth() + 5; // For example, 5 pixels padding
            int y = creditsButton.getY();

            String message = "Creati's Integration"; // The button text


            Button myButton = Button.builder(Component.literal(message), b -> {
                Minecraft.getInstance().setScreen(new ConfigScreen(screen));
            }).bounds(x, y, 210, 20).build();
            screen.renderables.add(myButton);
            event.addListener(myButton);

        }
    }

}
