package com.isaiahcreati.creatibotintegration.handlers;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.screens.ConfigScreen;
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
import java.util.UUID;


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
        if (!event.getLevel().isClientSide()) return;
        Component message;
        if(this.isConfigSetup()){
            message = Component.literal("[Creati's Integration] Welcome! Start receiving alerts with /start & /stop")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")));
        }else{
            message = Component.literal("[Warning] Creati's Integration not configured!")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55")));
        }
        player.sendSystemMessage(message);

        shown = true;
    }

    static public boolean isConfigSetup() {
        String alertKey = Config.ALERT_KEY.get();

        try {
            UUID id = UUID.fromString(alertKey);
            return true;
        } catch (IllegalArgumentException e) {
            // The string is not a valid UUID
            return false;
        }
    }


}
