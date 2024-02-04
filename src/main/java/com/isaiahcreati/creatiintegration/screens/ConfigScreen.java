package com.isaiahcreati.creatiintegration.screens;

import com.isaiahcreati.creatiintegration.Config;
import com.isaiahcreati.creatiintegration.inputs.ToggleButton;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.function.Supplier;


@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends Screen {

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final int BUTTON_WIDTH = 150;

    private static final Component TITLE = Component.literal("Creati's Bot Integration Settings");

    private static final Component TEST_BUTTON = Component.literal("Test Button");


    private final Screen lastScreen;
    private String actualText;
    private boolean isTextEditable = true;



    private EditBox uuidTextField;


    public ConfigScreen(Screen lastScreen) {
        super(TITLE);
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();

        int X1 = this.width / 2 - 155;
        int X2 = X1 + 160;
        int Y = this.height / 6 + 24;
        actualText = Config.ALERT_KEY.get(); // Retrieve the actual text from your config
        uuidTextField = new EditBox(this.font, X1, Y, BUTTON_WIDTH * 2 - 60, 20, Component.literal("Alert Key"));
        uuidTextField.setValue(actualText); // Set the current UUID from the config
        uuidTextField.setEditable(isTextEditable);
        uuidTextField.setResponder(newText -> {
            LOGGER.info("IS editeable:" + isTextEditable + ", newText: " + newText);
            if (!isTextEditable) return;
            actualText = newText;
        });
        this.addRenderableWidget(uuidTextField);

        ToggleButton AlertKeyToggle = new ToggleButton(X2 + (BUTTON_WIDTH - 60), Y, 60, 20, Component.literal("Show"), Component.literal("Hide"), isTextEditable, b -> {
            if (isTextEditable) {
                isTextEditable = false;
                // Hide the text
                actualText = uuidTextField.getValue(); // Update actualText with the current value
                uuidTextField.setValue("********-****-****-****-************");

            } else {
                isTextEditable = true;
                // Show the actual text
                uuidTextField.setValue(actualText);

            }

        }, Supplier::get);
        this.addRenderableWidget(AlertKeyToggle);


        Y += 24;
        this.addRenderableWidget(Button.builder(TEST_BUTTON, (b) -> {
            LOGGER.info("Pressed: " + b.hashCode());
        }).bounds(X1, Y, BUTTON_WIDTH, 20).build());
        this.addRenderableWidget(Button.builder(TEST_BUTTON, (b) -> {
            LOGGER.info("Pressed: " + b.hashCode());
        }).bounds(X2, Y, BUTTON_WIDTH, 20).build());
        Y += 48;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (p_280845_) -> {
            this.onClose();
        }).bounds(this.width / 2 - 100, Y, 200, 20).build());


    }

    private void save(){
        Config.ALERT_KEY.set(actualText);
        Config.CLIENT_CONFIG.save();
    }

    @Override
    public void onClose() {
        this.save();

        this.minecraft.setScreen(this.lastScreen);
    }


    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics); // This is typically called to render the default background
        super.render(guiGraphics, mouseX, mouseY, partialTick); // Call the super method to render buttons and other elements

        //Draw title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);

        int textY = uuidTextField.getY() - 15; // Position the text above the EditBox
        //Draw Alert Key text
        guiGraphics.drawString(this.font, Component.literal("Alert Key").getVisualOrderText(), uuidTextField.getX(), textY, 0xA0A0A0);

    }

}
