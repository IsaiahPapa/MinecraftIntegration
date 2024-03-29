package com.isaiahcreati.creatibotintegration.inputs;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class ToggleButton extends Button {
    private boolean toggled;
    private final Component onText;
    private final Component offText;

    public ToggleButton(int x, int y, int width, int height, Component onText, Component offText, boolean initialState, Button.OnPress onPress, CreateNarration pCreateNarration) {
        super(x, y, width, height, initialState ? onText : offText, onPress, pCreateNarration);
        this.toggled = initialState;
        this.onText = onText;
        this.offText = offText;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, float p_93660_) {
        super.render(guiGraphics, x, y, p_93660_);
    }

    public boolean isToggled() {
        return toggled;
    }

    public void toggle() {
        this.toggled = !this.toggled;
        this.setMessage(this.toggled ? this.onText : this.offText);
    }
}
