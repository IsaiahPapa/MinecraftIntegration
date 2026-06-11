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
                        .build())
                .save(() -> Config.CLIENT_CONFIG.save())
                .build()
                .generateScreen(parent);
    }
}