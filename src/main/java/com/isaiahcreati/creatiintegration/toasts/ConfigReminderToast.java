package com.isaiahcreati.creatiintegration.toasts;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.advancements.FrameType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ConfigReminderToast implements Toast {
    private final Component title;
    private final Component subtitle;
    private boolean playedSound;
    private static final int TEXT_COLOR = 0xFFFFFF; // White color


    public ConfigReminderToast(Component title, Component subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }


    @Override
    public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long delta) {
        Minecraft mc = toastComponent.getMinecraft();

        mc.getTextureManager().bindForSetup(TEXTURE);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, 0, 0, 0, 0, this.width() + 32, this.height());

        int iconSize = 16;
        int iconPadding = 6;
        ItemStack barrierItem = new ItemStack(Items.BARRIER);
        guiGraphics.renderFakeItem(barrierItem, 6, 6);
        int textOffsetX = iconPadding + iconSize + iconPadding;

        if (title != null) {
            int titleWidth = mc.font.width(title);
            guiGraphics.drawString(mc.font, title.getVisualOrderText(), textOffsetX + ((this.width() - titleWidth) / 2), 7, TEXT_COLOR);
        }

        if (subtitle != null) {
            int subtitleWidth = mc.font.width(subtitle);
            guiGraphics.drawString(mc.font, subtitle.getVisualOrderText(), textOffsetX + ((this.width() - subtitleWidth) / 2), 18, TEXT_COLOR);
        }
        if (!this.playedSound && delta > 0L) {
            this.playedSound = true;
            toastComponent.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.get(), 1.0F, 1.0F));
        }

        return delta >= 45000.0 * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW; // Show for 5 seconds
    }

}
