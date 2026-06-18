package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.Config;
import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = CreatiIntegration.MODID, value = Dist.CLIENT)
public class ClientScreenEventHandler {

    private static final int BUTTON_SPACING = 24;

    @SubscribeEvent
    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        if (!Config.REMOVE_REALMS_BUTTON.get()) return;
        if (!(event.getScreen() instanceof TitleScreen)) return;

        Component realmsText = Component.translatable("menu.online");
        int realmsY = -1;

        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button button && button.getMessage().equals(realmsText)) {
                realmsY = button.getY();
                event.removeListener(button);
                break;
            }
        }

        if (realmsY >= 0) {
            for (GuiEventListener listener : event.getListenersList()) {
                if (listener instanceof AbstractWidget widget && widget.getY() > realmsY) {
                    widget.setY(widget.getY() - BUTTON_SPACING);
                }
            }
        }
    }
}
