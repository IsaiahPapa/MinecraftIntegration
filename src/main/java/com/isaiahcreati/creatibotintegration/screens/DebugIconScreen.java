package com.isaiahcreati.creatibotintegration.screens;

import com.isaiahcreati.creatibotintegration.client.ActivityFeedRenderer;
import com.isaiahcreati.creatibotintegration.client.ActivityFeedState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;

import java.util.function.BiConsumer;

public class DebugIconScreen extends Screen {

    private static final int SLIDER_WIDTH = 200;
    private static final int SLIDER_HEIGHT = 20;
    private static final int BTN_W = 95;
    private static final int BTN_H = 20;

    private float scale3d = 29.5f;
    private float offsetY3d = 1.65f;
    private float scale2d = 1.375f;
    private int offsetX2d = 0;
    private int offsetY2d = 0;
    private boolean showBoundingBox = true;
    private int iconType = 0;
    private String testId = "minecraft:tnt";

    private CallbackSlider scale3dSlider;
    private CallbackSlider offsetY3dSlider;
    private CallbackSlider scale2dSlider;
    private CallbackSlider offsetX2dSlider;
    private CallbackSlider offsetY2dSlider;

    private static final String[][] ITEM_PRESETS = {
            {"TNT", "minecraft:tnt"},
            {"Ladder", "minecraft:ladder"},
            {"Chest", "minecraft:chest"},
            {"Door", "minecraft:oak_door"},
            {"Bed", "minecraft:red_bed"},
            {"Cake", "minecraft:cake"},
            {"Anvil", "minecraft:anvil"},
            {"Skull", "minecraft:zombie_head"},
    };

    private static final String[][] MOB_PRESETS = {
            {"Creeper", "minecraft:creeper"},
            {"Zombie", "minecraft:zombie"},
            {"Skeleton", "minecraft:skeleton"},
            {"Spider", "minecraft:spider"},
            {"Chicken", "minecraft:chicken"},
            {"Slime", "minecraft:slime"},
            {"Enderman", "minecraft:enderman"},
            {"Pig", "minecraft:pig"},
    };

    public DebugIconScreen() {
        super(Component.literal("Debug Icon Renderer"));
        if (ActivityFeedRenderer.debugItemScale >= 0) scale3d = ActivityFeedRenderer.debugItemScale;
        if (!Float.isNaN(ActivityFeedRenderer.debugItemOffsetY)) offsetY3d = ActivityFeedRenderer.debugItemOffsetY;
        if (!Float.isNaN(ActivityFeedRenderer.debug2dScale)) scale2d = ActivityFeedRenderer.debug2dScale;
        if (ActivityFeedRenderer.debug2dOffsetX != Integer.MIN_VALUE) offsetX2d = ActivityFeedRenderer.debug2dOffsetX;
        if (ActivityFeedRenderer.debug2dOffsetY != Integer.MIN_VALUE) offsetY2d = ActivityFeedRenderer.debug2dOffsetY;
        showBoundingBox = ActivityFeedRenderer.debugShowBoundingBox;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int y = 30;

        scale3dSlider = new CallbackSlider(centerX - SLIDER_WIDTH / 2, y, SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.literal("3D Scale: "), Component.literal(""), 0.5, 200, scale3d, 0.5, 1, true,
                (slider, value) -> { scale3d = value.floatValue(); updateDebug(); });
        this.addRenderableWidget(scale3dSlider);

        y += 30;
        offsetY3dSlider = new CallbackSlider(centerX - SLIDER_WIDTH / 2, y, SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.literal("3D OffY: "), Component.literal(""), -3.0, 3.0, offsetY3d, 0.01, 2, true,
                (slider, value) -> { offsetY3d = value.floatValue(); updateDebug(); });
        this.addRenderableWidget(offsetY3dSlider);

        y += 35;
        scale2dSlider = new CallbackSlider(centerX - SLIDER_WIDTH / 2, y, SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.literal("2D Scale: "), Component.literal(""), 0.5, 3.0, scale2d, 0.025, 3, true,
                (slider, value) -> { scale2d = value.floatValue(); updateDebug(); });
        this.addRenderableWidget(scale2dSlider);

        y += 30;
        offsetX2dSlider = new CallbackSlider(centerX - SLIDER_WIDTH / 2, y, SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.literal("2D OffX: "), Component.literal(""), -15, 15, offsetX2d, 1, 0, true,
                (slider, value) -> { offsetX2d = value.intValue(); updateDebug(); });
        this.addRenderableWidget(offsetX2dSlider);

        y += 30;
        offsetY2dSlider = new CallbackSlider(centerX - SLIDER_WIDTH / 2, y, SLIDER_WIDTH, SLIDER_HEIGHT,
                Component.literal("2D OffY: "), Component.literal(""), -15, 15, offsetY2d, 1, 0, true,
                (slider, value) -> { offsetY2d = value.intValue(); updateDebug(); });
        this.addRenderableWidget(offsetY2dSlider);

        y += 35;

        int cols = 4;
        for (int i = 0; i < ITEM_PRESETS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int bx = centerX - (cols * (BTN_W + 4)) / 2 + col * (BTN_W + 4);
            int by = y + row * (BTN_H + 3);
            String id = ITEM_PRESETS[i][1];
            this.addRenderableWidget(Button.builder(Component.literal(ITEM_PRESETS[i][0]), btn -> {
                iconType = 0; testId = id; updateDebug();
            }).bounds(bx, by, BTN_W, BTN_H).build());
        }

        y += (ITEM_PRESETS.length / cols) * (BTN_H + 3) + 10;

        for (int i = 0; i < MOB_PRESETS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int bx = centerX - (cols * (BTN_W + 4)) / 2 + col * (BTN_W + 4);
            int by = y + row * (BTN_H + 3);
            String id = MOB_PRESETS[i][1];
            this.addRenderableWidget(Button.builder(Component.literal(MOB_PRESETS[i][0]), btn -> {
                iconType = 1; testId = id; updateDebug();
            }).bounds(bx, by, BTN_W, BTN_H).build());
        }

        y += (MOB_PRESETS.length / cols) * (BTN_H + 3) + 10;

        this.addRenderableWidget(Button.builder(
                Component.literal(showBoundingBox ? "BBox: ON" : "BBox: OFF"),
                btn -> {
                    showBoundingBox = !showBoundingBox;
                    ActivityFeedRenderer.debugShowBoundingBox = showBoundingBox;
                    btn.setMessage(Component.literal(showBoundingBox ? "BBox: ON" : "BBox: OFF"));
                }).bounds(centerX - 110, y, 100, BTN_H).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset & Close"), btn -> {
            ActivityFeedRenderer.debugItemScale = -1f;
            ActivityFeedRenderer.debugItemOffsetY = Float.NaN;
            ActivityFeedRenderer.debug2dScale = Float.NaN;
            ActivityFeedRenderer.debug2dOffsetX = Integer.MIN_VALUE;
            ActivityFeedRenderer.debug2dOffsetY = Integer.MIN_VALUE;
            ActivityFeedRenderer.debugShowBoundingBox = false;
            ActivityFeedState.debugMode = false;
            ActivityFeedState.clear();
            this.onClose();
        }).bounds(centerX + 10, y, 100, BTN_H).build());

        ActivityFeedRenderer.debugShowBoundingBox = showBoundingBox;
        ActivityFeedState.debugMode = true;
        updateDebug();
    }

    private void updateDebug() {
        ActivityFeedRenderer.debugItemScale = scale3d;
        ActivityFeedRenderer.debugItemOffsetY = offsetY3d;
        ActivityFeedRenderer.debug2dScale = scale2d;
        ActivityFeedRenderer.debug2dOffsetX = offsetX2d;
        ActivityFeedRenderer.debug2dOffsetY = offsetY2d;
        ActivityFeedState.clear();
        if (iconType == 0) {
            ActivityFeedState.addNotification("TAUNT_INSTANT", testId.replace("minecraft:", ""), "DebugUser", "", 0, "item", testId);
        } else {
            ActivityFeedState.addNotification("SPAWN", "", "DebugUser", testId, 0, "entity", testId);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;

        String title = "Debug Icon Renderer";
        graphics.text(this.font, title, centerX - this.font.width(title) / 2, 8, 0xFFFFFFFF, true);

        String mode = iconType == 0
                ? (ActivityFeedRenderer.shouldRenderItemAs3D(testId) ? "[3D ArmorStand]" : "[2D Sprite]")
                : "[3D Mob]";
        String current = String.format("3D: scale=%.1f offY=%.2f | 2D: scale=%.3f off=%d,%d %s",
                scale3d, offsetY3d, scale2d, offsetX2d, offsetY2d, mode);
        graphics.text(this.font, current, centerX - this.font.width(current) / 2, this.height - 15, 0xFF55FF55, false);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        ActivityFeedState.debugMode = false;
        ActivityFeedState.clear();
        super.onClose();
    }

    private static class CallbackSlider extends ExtendedSlider {
        private final BiConsumer<CallbackSlider, Double> onValueChange;

        public CallbackSlider(int x, int y, int width, int height, Component prefix, Component suffix,
                              double minValue, double maxValue, double currentValue,
                              double stepSize, int precision, boolean drawString,
                              BiConsumer<CallbackSlider, Double> onValueChange) {
            super(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, stepSize, precision, drawString);
            this.onValueChange = onValueChange;
        }

        @Override
        protected void applyValue() {
            onValueChange.accept(this, getValue());
        }
    }
}