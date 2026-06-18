package com.isaiahcreati.creatibotintegration.client;

import java.util.ArrayList;
import java.util.List;

public class ActivityFeedState {

    public static final int TOAST_DURATION_MS = 5000;
    public static final int TOAST_FADE_MS = 1000;

    public static class ActivityToast {
        public final String eventType;
        public final String tauntId;
        public final String redeemerName;
        public final String extraInfo;
        public final int queuePosition;
        public final String iconType;
        public final String iconId;
        public final long createdAtMs;

        public ActivityToast(String eventType, String tauntId, String redeemerName, String extraInfo, int queuePosition, String iconType, String iconId) {
            this.eventType = eventType;
            this.tauntId = tauntId;
            this.redeemerName = redeemerName;
            this.extraInfo = extraInfo;
            this.queuePosition = queuePosition;
            this.iconType = iconType;
            this.iconId = iconId;
            this.createdAtMs = System.currentTimeMillis();
        }

        public ActivityToast(String eventType, String tauntId, String redeemerName, String extraInfo, int queuePosition) {
            this(eventType, tauntId, redeemerName, extraInfo, queuePosition, "", "");
        }

        public long ageMs() {
            return System.currentTimeMillis() - createdAtMs;
        }

        public boolean isExpired() {
            if (debugMode) return false;
            return ageMs() >= TOAST_DURATION_MS;
        }

        public float getOpacity() {
            long age = ageMs();
            long fadeStart = TOAST_DURATION_MS - TOAST_FADE_MS;
            if (age < fadeStart) return 1.0f;
            if (age >= TOAST_DURATION_MS) return 0.0f;
            return 1.0f - (float)(age - fadeStart) / TOAST_FADE_MS;
        }

        public boolean hasIcon() {
            return !iconType.isEmpty() && !iconId.isEmpty();
        }

        public String getDisplayName() {
            String key = tauntId.isEmpty() ? extraInfo : tauntId;
            String displayName = ClientQueueState.getDisplayName(key);
            if (!displayName.equals(key)) return displayName;
            return resolveDisplayName(key);
        }

        private static String resolveDisplayName(String key) {
            if (key.contains(":")) {
                try {
                    var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.tryParse(key));
                    if (itemId.isPresent()) {
                        return itemId.get().getDescriptionId();
                    }
                    var entityId = net.minecraft.world.entity.EntityType.byString(key);
                    if (entityId.isPresent()) {
                        return entityId.get().getDescription().getString();
                    }
                } catch (Exception e) {
                    // fall through
                }
            }
            return key;
        }
    }

    private static final List<ActivityToast> toasts = new ArrayList<>();
    private static final int MAX_VISIBLE = 5;

    public static boolean debugMode = false;

    public static void addNotification(String eventType, String tauntId, String redeemerName, String extraInfo, int queuePosition, String iconType, String iconId) {
        toasts.add(new ActivityToast(eventType, tauntId, redeemerName, extraInfo, queuePosition, iconType, iconId));
        while (toasts.size() > MAX_VISIBLE + 2) {
            toasts.remove(0);
        }
    }

    public static void tick() {
        toasts.removeIf(ActivityToast::isExpired);
        ActivityIconCache.tick();
    }

    public static List<ActivityToast> getToasts() {
        return toasts;
    }

    public static void clear() {
        toasts.clear();
    }
}