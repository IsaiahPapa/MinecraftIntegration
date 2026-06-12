package com.isaiahcreati.creatibotintegration.helpers;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.core.component.DataComponents;

import java.util.List;

public class OnboardingBook {

    private static final String BOOK_TITLE = "Creati's Bot";

    public static ItemStack create() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

        List<Filterable<Component>> pages = List.of(
                Filterable.passThrough(
                        Component.literal("")
                                .append(Component.literal("Welcome to\nCreatiBot Integration!\n\n").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#55AAFF").getOrThrow()).withBold(true)))
                                .append(Component.literal("This mod connects your Minecraft world to Creati's Bot for interactive stream taunts and minigames.\n\n").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA").getOrThrow())))
                                .append(Component.literal("[Click to Setup]").withStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF5555").getOrThrow()).withBold(true).withClickEvent(new ClickEvent.RunCommand("/creati setup"))))
                )
        );

        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough(BOOK_TITLE),
                "IsaiahCreati",
                0,
                pages,
                true
        );

        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    public static boolean isOnboardingBook(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(Items.WRITTEN_BOOK)) return false;
        WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (content == null) return false;
        return content.title().raw().equals(BOOK_TITLE);
    }
}