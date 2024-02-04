package com.isaiahcreati.creatiintegration.handlers;

import com.isaiahcreati.creatiintegration.screens.ConfigScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

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

    }
    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event)
    {
        if(shown) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.getLevel().isClientSide() || this.isConfigSetup()) return;

        Component message = Component.literal("[Warning] Creati's Integration not configured!")
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55")));
        player.sendSystemMessage(message);

        shown = true;
    }

    private boolean isConfigSetup() {
        return false;
    }

}
