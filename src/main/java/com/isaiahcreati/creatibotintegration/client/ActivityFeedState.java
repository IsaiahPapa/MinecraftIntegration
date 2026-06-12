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
        public final long createdAtMs;

        public ActivityToast(String eventType, String tauntId, String redeemerName, String extraInfo, int queuePosition) {
            this.eventType = eventType;
            this.tauntId = tauntId;
            this.redeemerName = redeemerName;
            this.extraInfo = extraInfo;
            this.queuePosition = queuePosition;
            this.createdAtMs = System.currentTimeMillis();
        }

        public long ageMs() {
            return System.currentTimeMillis() - createdAtMs;
        }

        public boolean isExpired() {
            return ageMs() >= TOAST_DURATION_MS;
        }

        public float getOpacity() {
            long age = ageMs();
            long fadeStart = TOAST_DURATION_MS - TOAST_FADE_MS;
            if (age < fadeStart) return 1.0f;
            if (age >= TOAST_DURATION_MS) return 0.0f;
            return 1.0f - (float)(age - fadeStart) / TOAST_FADE_MS;
        }

        public String getDisplayName() {
            return ClientQueueState.getDisplayName(tauntId.isEmpty() ? extraInfo : tauntId);
        }
    }

    private static final List<ActivityToast> toasts = new ArrayList<>();
    private static final int MAX_VISIBLE = 5;

    public static void addNotification(String eventType, String tauntId, String redeemerName, String extraInfo, int queuePosition) {
        toasts.add(new ActivityToast(eventType, tauntId, redeemerName, extraInfo, queuePosition));
        while (toasts.size() > MAX_VISIBLE + 2) {
            toasts.remove(0);
        }
    }

    public static void tick() {
        toasts.removeIf(ActivityToast::isExpired);
    }

    public static List<ActivityToast> getToasts() {
        return toasts;
    }

    public static void clear() {
        toasts.clear();
    }
}