package com.isaiahcreati.creatibotintegration.client;

import com.isaiahcreati.creatibotintegration.CreatiIntegration;
import com.isaiahcreati.creatibotintegration.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class ActivityFeedRenderer {

    public static final Identifier ACTIVITY_FEED_ID =
            Identifier.fromNamespaceAndPath("creatibotintegration", "activity_feed");

    private static final int PADDING = 3;
    private static final int LINE_HEIGHT = 9;
    private static final int TOAST_WIDTH_NO_ICON = 140;
    private static final int TOAST_WIDTH_WITH_ICON = 160;
    private static final int MAX_VISIBLE = 5;
    private static final int MARGIN_LEFT = 5;
    private static final int MARGIN_TOP = 5;
    private static final int SPACING = 1;
    private static final int ICON_SIZE = 30;

    private static final float DEFAULT_ITEM_SCALE = 29.5f;
    private static final float DEFAULT_ITEM_OFFSET_Y = 1.72f;
    private static final float DEFAULT_2D_SCALE = 1.5f;
    private static final int DEFAULT_2D_OFFSET_X = 0;
    private static final int DEFAULT_2D_OFFSET_Y = 0;

    public static float debugItemScale = -1f;
    public static float debugItemOffsetY = Float.NaN;
    public static float debug2dScale = Float.NaN;
    public static int debug2dOffsetX = Integer.MIN_VALUE;
    public static int debug2dOffsetY = Integer.MIN_VALUE;
    public static boolean debugShowBoundingBox = false;

    private static int getEventColor(String eventType) {
        return switch (eventType) {
            case "GIVE" -> 0x55AAFF;
            case "SPAWN" -> 0x55FF55;
            case "EFFECT" -> 0xAA55FF;
            case "TAUNT", "TAUNT_INSTANT" -> 0xFF5555;
            case "TAUNT_QUEUED", "MINIGAME_QUEUED" -> 0xFFAA00;
            case "TAUNT_ACTIVATED", "MINIGAME_ACTIVATED" -> 0x55FF55;
            case "TAUNT_EXTENDED" -> 0xFFFF55;
            case "VISUAL_EFFECT_QUEUED" -> 0xFFAA00;
            case "VISUAL_EFFECT_ACTIVATED" -> 0x55FFFF;
            default -> 0xFFFFFF;
        };
    }

    private static String getStatusLabel(String eventType) {
        return switch (eventType) {
            case "GIVE" -> "\u00a79Give";
            case "SPAWN" -> "\u00a7aSpawn";
            case "EFFECT" -> "\u00a75Effect";
            case "TAUNT", "TAUNT_INSTANT" -> "";
            case "TAUNT_QUEUED" -> "\u00a76Queued";
            case "TAUNT_ACTIVATED" -> "\u00a7aActive";
            case "TAUNT_EXTENDED" -> "\u00a7eExtended";
            case "MINIGAME_QUEUED" -> "\u00a76Queued";
            case "MINIGAME_ACTIVATED" -> "\u00a7aActive";
            case "VISUAL_EFFECT_QUEUED" -> "\u00a76Queued";
            case "VISUAL_EFFECT_ACTIVATED" -> "\u00a7bActive";
            default -> "";
        };
    }

    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        if (!Config.ACTIVITY_FEED_VISIBLE.get()) return;

        List<ActivityFeedState.ActivityToast> toasts = ActivityFeedState.getToasts();
        if (toasts.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Font font = mc.font;
        int y = MARGIN_TOP;
        int count = 0;

        for (ActivityFeedState.ActivityToast toast : toasts) {
            if (count >= MAX_VISIBLE) break;

            float opacity = toast.getOpacity();
            if (opacity <= 0f) continue;

            int alpha = (int)(opacity * 255);
            int bgColor = (alpha << 24);
            int borderColor = (alpha << 24) | (getEventColor(toast.eventType) & 0xFFFFFF);
            int eventColor = getEventColor(toast.eventType);

            String statusLabel = getStatusLabel(toast.eventType);
            String displayName = toast.getDisplayName();
            boolean hasIcon = toast.hasIcon();

            int lines = 1;
            if (!statusLabel.isEmpty()) lines++;
            if (!toast.redeemerName.isEmpty()) lines++;
            if (toast.queuePosition > 0) lines++;

            int toastWidth = hasIcon ? TOAST_WIDTH_WITH_ICON : TOAST_WIDTH_NO_ICON;
            int contentHeight = lines * LINE_HEIGHT;
            int boxHeight = Math.max(hasIcon ? ICON_SIZE : 0, contentHeight) + PADDING * 2;

            int boxX = MARGIN_LEFT;

            guiGraphics.fill(boxX, y, boxX + toastWidth, y + boxHeight, bgColor);
            guiGraphics.fill(boxX, y, boxX + 2, y + boxHeight, borderColor);

            int textX;
            if (hasIcon) {
                int iconPadX = PADDING + 2;
                int iconPadY = (boxHeight - ICON_SIZE) / 2;
                renderIcon(guiGraphics, mc, toast, boxX + iconPadX, y + iconPadY, ICON_SIZE, alpha);

                if (debugShowBoundingBox) {
                    int ibx = boxX + iconPadX;
                    int iby = y + iconPadY;
                    guiGraphics.fill(ibx, iby, ibx + ICON_SIZE, iby + 1, 0xFFFF0000);
                    guiGraphics.fill(ibx, iby + ICON_SIZE - 1, ibx + ICON_SIZE, iby + ICON_SIZE, 0xFFFF0000);
                    guiGraphics.fill(ibx, iby, ibx + 1, iby + ICON_SIZE, 0xFFFF0000);
                    guiGraphics.fill(ibx + ICON_SIZE - 1, iby, ibx + ICON_SIZE, iby + ICON_SIZE, 0xFFFF0000);
                }

                textX = boxX + ICON_SIZE + PADDING + 4;
            } else {
                textX = boxX + PADDING + 2;
            }

            int textY = y + PADDING;

            if (!statusLabel.isEmpty()) {
                guiGraphics.text(font, statusLabel, textX, textY, applyAlpha(0xAAAAAA, alpha), false);
                textY += LINE_HEIGHT;
            }

            guiGraphics.text(font, "\u00a7f" + displayName, textX, textY, applyAlpha(eventColor, alpha), false);
            textY += LINE_HEIGHT;

            if (!toast.redeemerName.isEmpty()) {
                guiGraphics.text(font, "\u00a78by " + toast.redeemerName, textX, textY, applyAlpha(0x888888, alpha), false);
                textY += LINE_HEIGHT;
            }

            if (toast.queuePosition > 0) {
                guiGraphics.text(font, "\u00a78#" + toast.queuePosition, textX, textY, applyAlpha(0x666666, alpha), false);
            }

            y += boxHeight + SPACING;
            count++;
        }
    }

    private static void renderIcon(GuiGraphicsExtractor guiGraphics, Minecraft mc, ActivityFeedState.ActivityToast toast, int x, int y, int size, int alpha) {
        if (toast.iconType.equals("entity")) {
            renderEntityIcon(guiGraphics, mc, toast.iconId, x, y, size, false);
        } else if (toast.iconType.equals("item")) {
            boolean use3d = shouldRenderItemAs3D(toast.iconId);
            LivingEntity itemEntity = use3d ? ActivityIconCache.getOrCreateItemEntity(toast.iconId) : null;
            if (itemEntity != null) {
                renderEntityIconDirect(guiGraphics, mc, itemEntity, x, y, size, true);
            } else {
                renderItemIconFallback(guiGraphics, mc, toast.iconId, x, y, size);
            }
        }
    }

    public static boolean shouldRenderItemAs3D(String itemId) {
        var itemOpt = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(itemId));
        if (itemOpt.isEmpty()) return false;
        Item item = itemOpt.get();
        if (!(item instanceof BlockItem blockItem)) return false;

        BlockState defaultState = blockItem.getBlock().defaultBlockState();
        VoxelShape shape = defaultState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (shape.isEmpty()) return false;

        double widthX = shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X);
        double heightY = shape.max(Direction.Axis.Y) - shape.min(Direction.Axis.Y);
        double widthZ = shape.max(Direction.Axis.Z) - shape.min(Direction.Axis.Z);

        double minDim = Math.min(widthX, Math.min(heightY, widthZ));
        double maxDim = Math.max(widthX, Math.max(heightY, widthZ));

        return minDim >= 0.4 && (maxDim / minDim) <= 1.5;
    }

    private static void renderEntityIcon(GuiGraphicsExtractor guiGraphics, Minecraft mc, String entityTypeId, int x, int y, int size, boolean zoomToHead) {
        LivingEntity entity = ActivityIconCache.getOrCreateEntity(entityTypeId);
        if (entity == null) return;
        renderEntityIconDirect(guiGraphics, mc, entity, x, y, size, zoomToHead);
    }

    private static void renderEntityIconDirect(GuiGraphicsExtractor guiGraphics, Minecraft mc, LivingEntity entity, int x, int y, int size, boolean zoomToHead) {
        float spinAngle = ActivityIconCache.getSpinAngle();

        try {
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
            EntityRenderer<? super LivingEntity, ?> renderer = (EntityRenderer<? super LivingEntity, ?>) dispatcher.getRenderer(entity);
            if (renderer == null) return;

            EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
            renderState.shadowPieces.clear();
            renderState.outlineColor = 0;

            if (renderState instanceof LivingEntityRenderState livingState) {
                livingState.bodyRot = 180.0F;
                livingState.yRot = 0.0F;
                livingState.xRot = 0.0F;
                float scale = livingState.scale;
                if (scale > 0) {
                    livingState.boundingBoxWidth = livingState.boundingBoxWidth / scale;
                    livingState.boundingBoxHeight = livingState.boundingBoxHeight / scale;
                }
                livingState.scale = 1.0F;
            }

            float renderScale;
            float offsetY;

            if (zoomToHead) {
                renderScale = debugItemScale >= 0 ? debugItemScale : DEFAULT_ITEM_SCALE;
                offsetY = !Float.isNaN(debugItemOffsetY) ? debugItemOffsetY : DEFAULT_ITEM_OFFSET_Y;
            } else {
                renderScale = size / (renderState.boundingBoxHeight > 0 ? renderState.boundingBoxHeight : 1.0f);
                if (renderScale > 25f) renderScale = 25f;
                if (renderScale < 3f) renderScale = 3f;
                offsetY = renderState.boundingBoxHeight / 2.0f;
            }

            Vector3f translation = new Vector3f(0.0f, offsetY, 0.0f);

            Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
            rotation.rotateY((float) Math.toRadians(spinAngle));

            int x0 = x;
            int y0 = y;
            int x1 = x + size;
            int y1 = y + size;

            guiGraphics.entity(renderState, renderScale, translation, rotation, null, x0, y0, x1, y1);
        } catch (Exception e) {
            CreatiIntegration.LOGGER.warn("Failed to render entity icon", e);
        }
    }

    private static void renderItemIconFallback(GuiGraphicsExtractor guiGraphics, Minecraft mc, String itemId, int x, int y, int size) {
        try {
            var itemOpt = BuiltInRegistries.ITEM.getOptional(Identifier.tryParse(itemId));
            if (itemOpt.isEmpty()) return;
            ItemStack itemStack = new ItemStack(itemOpt.get());

            float scale = !Float.isNaN(debug2dScale) ? debug2dScale : DEFAULT_2D_SCALE;
            int offX = debug2dOffsetX != Integer.MIN_VALUE ? debug2dOffsetX : DEFAULT_2D_OFFSET_X;
            int offY = debug2dOffsetY != Integer.MIN_VALUE ? debug2dOffsetY : DEFAULT_2D_OFFSET_Y;

            float halfSize = size / 2f;
            float itemHalf = 8f * scale;

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(x + halfSize + offX, y + halfSize + offY);
            guiGraphics.pose().scale(scale, scale);
            guiGraphics.pose().translate(-8, -8);
            guiGraphics.item(itemStack, 0, 0);
            guiGraphics.pose().popMatrix();
        } catch (Exception e) {
            CreatiIntegration.LOGGER.warn("Failed to render item icon fallback for: {}", itemId, e);
        }
    }

    private static int applyAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}