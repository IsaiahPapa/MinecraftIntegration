package com.isaiahcreati.creatibotintegration.handlers;

import com.isaiahcreati.creatibotintegration.Config;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.UUID;


@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class EventHandler {
    public static final Logger LOGGER = LogUtils.getLogger();
    private boolean shown = false;

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (shown) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.getLevel().isClientSide()) return;
        Component message;
        if (isConfigSetup()) {
            message = Component.literal("[Creati's Integration] Welcome! Start receiving alerts with /start & /stop")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFFFF")));
        } else {
            message = Component.literal("[Warning] Creati's Integration not configured!")
                    .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFFF55")));
        }
        player.sendSystemMessage(message);
        shown = true;
    }

    public static boolean isConfigSetup() {
        String alertKey = Config.ALERT_KEY.get();
        try {
            UUID.fromString(alertKey);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}