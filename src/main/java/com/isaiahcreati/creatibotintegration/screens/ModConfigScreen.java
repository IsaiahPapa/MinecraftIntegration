package com.isaiahcreati.creatibotintegration.screens;

import com.isaiahcreati.creatibotintegration.Config;
import dev.isxander.yacl3.api.Binding;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModConfigScreen {

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Component.literal("Creati's Bot Integration"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("General"))
                        .option(Option.<String>createBuilder()
                                .name(Component.literal("Alert Key"))
                                .description(OptionDescription.of(Component.literal("Your Alert Key from Creati's Bot dashboard")))
                                .binding(Binding.generic("", Config.ALERT_KEY::get, Config.ALERT_KEY::set))
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Chat Alerts"))
                                .description(OptionDescription.of(Component.literal("Show chat alerts when taunts are triggered")))
                                .binding(Binding.generic(true, Config.CHAT_ALERTS::get, Config.CHAT_ALERTS::set))
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Auto Connect"))
                                .description(OptionDescription.of(Component.literal("Automatically connect to the bot server on world load")))
                                .binding(Binding.generic(true, Config.AUTO_CONNECT::get, Config.AUTO_CONNECT::set))
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("Minigames"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Parkour"))
                                .description(OptionDescription.of(Component.literal("Allow viewers to send you to a parkour course")))
                                .binding(Binding.generic(true, Config.PARKOUR_ENABLED::get, Config.PARKOUR_ENABLED::set))
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("TNT Run"))
                                .description(OptionDescription.of(Component.literal("Allow viewers to send you to a TNT Run arena")))
                                .binding(Binding.generic(true, Config.TNT_RUN_ENABLED::get, Config.TNT_RUN_ENABLED::set))
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Dropper"))
                                .description(OptionDescription.of(Component.literal("Allow viewers to send you to a dropper tower")))
                                .binding(Binding.generic(true, Config.DROPPER_ENABLED::get, Config.DROPPER_ENABLED::set))
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Component.literal("On-Screen Display"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Queue Sidebar"))
                                .description(OptionDescription.of(Component.literal("Show the active effect/queue sidebar in the corner of your screen")))
                                .binding(Binding.generic(true, Config.SIDEBAR_VISIBLE::get, Config.SIDEBAR_VISIBLE::set))
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.literal("Activity Feed"))
                                .description(OptionDescription.of(Component.literal("Show toast notifications in the corner when redeems fire")))
                                .binding(Binding.generic(true, Config.ACTIVITY_FEED_VISIBLE::get, Config.ACTIVITY_FEED_VISIBLE::set))
                                .controller(TickBoxControllerBuilder::create)
                                .build())
                        .build())
                .save(() -> Config.CLIENT_CONFIG.save())
                .build()
                .generateScreen(parent);
    }
}